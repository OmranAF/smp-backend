import 'dart:convert';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:http/http.dart' as http;
import 'package:http/http.dart';

import 'models.dart';

class ApiClient {
  ApiClient(this.baseUrl);

  final String baseUrl;

  Future<AppUser> loginAuto(String email, String password) async {
    try {
      return await loginPatient(email, password);
    } on _LoginFailure catch (patientFailure) {
      if (patientFailure.statusCode != 401) {
        rethrow;
      }

      try {
        return await loginDoctor(email, password);
      } on _LoginFailure catch (doctorFailure) {
        if (doctorFailure.statusCode == 401) {
          throw patientFailure;
        }
        rethrow;
      }
    }
  }

  Future<AppUser> loginDoctor(String email, String password) async {
    final data = await _loginJson('/api/doctors/login', email, password, 'Doctor');
    return AppUser(
      id: data['doctorId'].toString(),
      name: data['name'].toString(),
      email: data['email'].toString(),
      role: AppRole.doctor,
    );
  }

  Future<AppUser> loginPatient(String email, String password) async {
    final data = await _loginJson('/api/patients/login', email, password, 'Patient');
    return AppUser(
      id: data['patientId'].toString(),
      name: data['name'].toString(),
      email: data['email'].toString(),
      role: AppRole.patient,
    );
  }

  Future<List<DoctorOption>> getDoctors({required String authHeader}) async {
    final uri = Uri.parse('$baseUrl/api/doctors');
    final response = await http.get(
      uri,
      headers: {'Authorization': authHeader},
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load doctors failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected doctors response format');
    }

    return data.map((item) => DoctorOption.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<DoctorServiceOption>> getDoctorServices({
    required String authHeader,
    required String doctorId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/services');
    final response = await http.get(
      uri,
      headers: {'Authorization': authHeader},
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load doctor services failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected doctor services response format');
    }

    return data.map((item) => DoctorServiceOption.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<FreeAppointmentSlot>> getDoctorFreeSlots({
    required String authHeader,
    required String doctorId,
    required DateTime date,
  }) async {
    final dateParam = date.toIso8601String().split('T').first;
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/free-slots?date=$dateParam');
    final response = await http.get(
      uri,
      headers: {'Authorization': authHeader},
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load free slots failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected free slots response format');
    }

    return data.map((item) => FreeAppointmentSlot.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<AppointmentSummary> createAppointment({
    required String authHeader,
    required String doctorId,
    required String patientId,
    required String serviceId,
    required DateTime appointmentTime,
    String? reason,
    String? notes,
  }) async {
    final uri = Uri.parse('$baseUrl/api/appointments');
    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode({
        'doctorId': doctorId,
        'patientId': patientId,
        'serviceId': serviceId,
        'appointmentTime': appointmentTime.toIso8601String(),
        'reason': reason,
        'notes': notes,
      }),
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Create appointment failed: ${data['message'] ?? response.body}');
    }

    return AppointmentSummary.fromJson(data as Map<String, dynamic>);
  }

  Future<List<AppointmentSummary>> getDoctorAppointments({
    required String authHeader,
    required String doctorId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/appointments');
    final response = await http.get(
      uri,
      headers: {'Authorization': authHeader},
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load doctor appointments failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected doctor appointments response format');
    }

    return data.map((item) => AppointmentSummary.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<DoctorPrescription>> getDoctorPrescriptions({
    required String authHeader,
    required String doctorId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/prescriptions');
    final response = await http.get(
      uri,
      headers: {'Authorization': authHeader},
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load doctor prescriptions failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected doctor prescriptions response format');
    }

    return data.map((item) => DoctorPrescription.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<DoctorPrescription> createDoctorPrescription({
    required String authHeader,
    required String doctorId,
    required String patientId,
    String? appointmentId,
    required String medicationName,
    required String dosage,
    required String instructions,
    DateTime? validUntil,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/prescriptions');
    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode({
        'patientId': patientId,
        'appointmentId': appointmentId,
        'medicationName': medicationName,
        'dosage': dosage,
        'instructions': instructions,
        'validUntil': validUntil == null ? null : validUntil.toIso8601String().split('T').first,
      }),
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Create doctor prescription failed: ${data['message'] ?? response.body}');
    }

    return DoctorPrescription.fromJson(data as Map<String, dynamic>);
  }

  Future<List<DoctorAvailability>> getDoctorAvailabilities({
    required String authHeader,
    required String doctorId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/availabilities');
    final response = await http.get(
      uri,
      headers: {'Authorization': authHeader},
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load availabilities failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected availabilities response format');
    }

    return data.map((item) => DoctorAvailability.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<DoctorAvailability> createDoctorAvailability({
    required String authHeader,
    required String doctorId,
    required String dayOfWeek,
    required String startTime,
    required String endTime,
    required int slotDurationMinutes,
    bool active = true,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/availabilities');
    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode({
        'dayOfWeek': dayOfWeek,
        'startTime': startTime,
        'endTime': endTime,
        'slotDurationMinutes': slotDurationMinutes,
        'active': active,
      }),
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Create availability failed: ${data['message'] ?? response.body}');
    }

    return DoctorAvailability.fromJson(data as Map<String, dynamic>);
  }

  Future<void> deleteDoctorAvailability({
    required String authHeader,
    required String doctorId,
    required String availabilityId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/doctors/$doctorId/availabilities/$availabilityId');
    final response = await http.delete(
      uri,
      headers: {'Authorization': authHeader},
    );

    if (response.statusCode >= 400) {
      final data = _parseJson(response);
      throw Exception('Delete availability failed: ${data['message'] ?? response.body}');
    }
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

  Future<List<PatientPrescription>> getPatientPrescriptions({
    required String authHeader,
    required String patientId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/patients/$patientId/prescriptions');
    final response = await http.get(
      uri,
      headers: {
        'Authorization': authHeader,
      },
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load prescriptions failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected prescriptions response format');
    }

    return data.map((item) => PatientPrescription.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<PatientDocument>> getPatientDocuments({
    required String authHeader,
    required String patientId,
  }) async {
    final uri = Uri.parse('$baseUrl/api/patients/$patientId/documents');
    final response = await http.get(
      uri,
      headers: {
        'Authorization': authHeader,
      },
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load documents failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected documents response format');
    }

    return data.map((item) => PatientDocument.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<PatientDocument>> uploadPatientDocuments({
    required String authHeader,
    required String patientId,
    required List<PlatformFile> files,
    String? description,
  }) async {
    final uri = Uri.parse('$baseUrl/api/patients/$patientId/documents');
    final request = http.MultipartRequest('POST', uri);
    request.headers['Authorization'] = authHeader;

    for (final file in files) {
      final bytes = file.bytes;
      if (bytes == null) {
        throw Exception('Could not read ${file.name}');
      }

      request.files.add(
        http.MultipartFile.fromBytes(
          'files',
          bytes,
          filename: file.name,
        ),
      );
    }

    final normalizedDescription = description?.trim();
    if (normalizedDescription != null && normalizedDescription.isNotEmpty) {
      request.fields['description'] = normalizedDescription;
    }

    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    final data = _parseJson(response);

    if (response.statusCode >= 400) {
      throw Exception('Upload documents failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected upload response format');
    }

    return data.map((item) => PatientDocument.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<AllergyOption>> getAllergies({required String authHeader}) async {
    final uri = Uri.parse('$baseUrl/api/allergies');
    final response = await http.get(
      uri,
      headers: {
        'Authorization': authHeader,
      },
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Load allergies failed: ${data['message'] ?? response.body}');
    }

    if (data is! List) {
      throw Exception('Unexpected allergies response format');
    }

    return data.map((item) => AllergyOption.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<void> setPatientAllergies({
    required String authHeader,
    required String patientId,
    required List<String> allergyIds,
  }) async {
    final uri = Uri.parse('$baseUrl/api/patients/$patientId/allergies');
    final response = await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode(allergyIds),
    );

    if (response.statusCode >= 400) {
      final data = _parseJson(response);
      throw Exception('Save allergies failed: ${data['message'] ?? response.body}');
    }
  }

  Future<PatientHealthStatus> updatePatientHealthStatus({
    required String authHeader,
    required String patientId,
    required String status,
    String? notes,
  }) async {
    final uri = Uri.parse('$baseUrl/api/patients/$patientId/health-status');
    final response = await http.put(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader,
      },
      body: jsonEncode({
        'status': status,
        'notes': notes,
      }),
    );

    final data = _parseJson(response);
    if (response.statusCode >= 400) {
      throw Exception('Update health status failed: ${data['message'] ?? response.body}');
    }

    return PatientHealthStatus.fromJson(data as Map<String, dynamic>);
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

  Future<Map<String, dynamic>> _loginJson(
    String path,
    String email,
    String password,
    String label,
  ) async {
    final uri = Uri.parse('$baseUrl$path');
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
      throw _LoginFailure(
        label: label,
        statusCode: response.statusCode,
        message: data['message']?.toString() ?? response.body,
      );
    }

    return data as Map<String, dynamic>;
  }
}

class _LoginFailure implements Exception {
  const _LoginFailure({
    required this.label,
    required this.statusCode,
    required this.message,
  });

  final String label;
  final int statusCode;
  final String message;

  @override
  String toString() => '$label login failed: $message';
}
