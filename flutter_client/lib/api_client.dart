import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:http/http.dart';

import 'models.dart';

class ApiClient {
  ApiClient(this.baseUrl);

  final String baseUrl;

  Future<AppUser> loginDoctor(String email, String password) async {
    final uri = Uri.parse('$baseUrl/api/doctors/login');
    http.Response response;
    try {
      response = await http.post(
        uri,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
    } on ClientException {
      throw Exception(_networkHelp(baseUrl));
    }

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Doctor login failed: ${data['message'] ?? response.body}');
    }

    return AppUser(
      id: data['doctorId'].toString(),
      name: data['name'].toString(),
      email: data['email'].toString(),
      role: AppRole.doctor,
    );
  }

  Future<AppUser> loginPatient(String email, String password) async {
    final uri = Uri.parse('$baseUrl/api/patients/login');
    http.Response response;
    try {
      response = await http.post(
        uri,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
    } on ClientException {
      throw Exception(_networkHelp(baseUrl));
    }

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Patient login failed: ${data['message'] ?? response.body}');
    }

    return AppUser(
      id: data['patientId'].toString(),
      name: data['name'].toString(),
      email: data['email'].toString(),
      role: AppRole.patient,
    );
  }

  Future<ConversationThread> createOrGetThread({
    required String authHeader,
    required String doctorId,
    required String patientId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/conversations/threads');
    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode({'doctorId': doctorId, 'patientId': patientId}),
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Open thread failed: ${data['message'] ?? response.body}');
    }

    return ConversationThread(
      id: data['id'].toString(),
      doctorId: data['doctorId'].toString(),
      patientId: data['patientId'].toString(),
    );
  }

  Future<List<ConversationPartner>> getConversationPartners({
    required String authHeader,
  }) async {
    final uri = Uri.parse('$baseUrl/api/conversations/partners');
    final response = await http.get(
      uri,
      headers: {
        'Authorization': authHeader,
      },
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load partners failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected partners response format');
    }

    return data
        .map((item) => ConversationPartner.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<ChatMessage>> getMessages({
    required String authHeader,
    required String conversationId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/conversations/threads/$conversationId/messages');
    final response = await http.get(
      uri,
      headers: {
        'Authorization': authHeader,
      },
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load messages failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected messages response format');
    }

    return data
        .map((item) => ChatMessage.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<void> sendTextMessage({
    required String authHeader,
    required String conversationId,
    required String content,
  }) async {
    final uri = Uri.parse('$baseUrl/api/conversations/threads/$conversationId/messages');
    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode({'content': content}),
    );

    if (response.statusCode >= 400) {
      final data = _parseJson(response);
      throw Exception('Send message failed: ${data['message'] ?? response.body}');
    }
  }

  Future<void> sendAttachment({
    required String authHeader,
    required String conversationId,
    required Uint8List bytes,
    required String filename,
    String? content,
  }) async {
    final uri = Uri.parse('$baseUrl/api/conversations/threads/$conversationId/attachments');
    final request = http.MultipartRequest('POST', uri);
    request.headers['Authorization'] = authHeader;
    request.files.add(
      http.MultipartFile.fromBytes(
        'file',
        bytes,
        filename: filename,
      ),
    );
    if (content != null && content.trim().isNotEmpty) {
      request.fields['content'] = content.trim();
    }

    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);

    if (response.statusCode >= 400) {
      final data = _parseJson(response);
      throw Exception('Send attachment failed: ${data['message'] ?? response.body}');
    }
  }

  Future<void> saveAttachmentToPatientProfile({
    required String authHeader,
    required String conversationId,
    required String attachmentId,
  }) async {
    final uri = Uri.parse(
      '$baseUrl/api/conversations/threads/$conversationId/attachments/$attachmentId/save-to-patient-profile',
    );
    final response = await http.post(
      uri,
      headers: {'Authorization': authHeader},
    );

    if (response.statusCode >= 400) {
      final data = _parseJson(response);
      throw Exception('Save to profile failed: ${data['message'] ?? response.body}');
    }
  }

  dynamic _parseJson(http.Response response) {
    if (response.body.isEmpty) return <String, dynamic>{};
    try {
      return jsonDecode(response.body);
    } catch (_) {
      return <String, dynamic>{'message': response.body};
    }
  }

  String _networkHelp(String url) {
    return 'Could not reach backend at $url. Ensure Spring Boot is running on port 8080 and use the right URL for your target (Web: http://localhost:8080, Android emulator: http://10.0.2.2:8080).';
  }
}
