enum AppRole { doctor, patient }

class AppUser {
  const AppUser({
    required this.id,
    required this.name,
    required this.email,
    required this.role,
  });

  final String id;
  final String name;
  final String email;
  final AppRole role;
}

class ConversationThread {
  const ConversationThread({
    required this.id,
    required this.doctorId,
    required this.patientId,
  });

  final String id;
  final String doctorId;
  final String patientId;
}

class ConversationPartner {
  const ConversationPartner({
    required this.id,
    required this.name,
    required this.email,
    required this.subtitle,
  });

  final String id;
  final String name;
  final String email;
  final String subtitle;

  factory ConversationPartner.fromJson(Map<String, dynamic> json) {
    return ConversationPartner(
      id: json['id'].toString(),
      name: json['name']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      subtitle: json['subtitle']?.toString() ?? '',
    );
  }
}

class AttachmentMeta {
  const AttachmentMeta({
    required this.id,
    required this.fileName,
    required this.contentType,
    required this.fileSize,
    required this.savedToPatientProfile,
  });

  final String id;
  final String fileName;
  final String contentType;
  final int fileSize;
  final bool savedToPatientProfile;

  factory AttachmentMeta.fromJson(Map<String, dynamic> json) {
    return AttachmentMeta(
      id: json['id'].toString(),
      fileName: json['fileName']?.toString() ?? 'file',
      contentType: json['contentType']?.toString() ?? 'application/octet-stream',
      fileSize: (json['fileSize'] as num?)?.toInt() ?? 0,
      savedToPatientProfile: json['savedToPatientProfile'] == true,
    );
  }
}

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.senderRole,
    required this.senderName,
    required this.content,
    required this.createdAt,
    required this.attachments,
  });

  final String id;
  final String senderRole;
  final String senderName;
  final String? content;
  final String createdAt;
  final List<AttachmentMeta> attachments;

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    final rawAttachments = (json['attachments'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(AttachmentMeta.fromJson)
        .toList();

    return ChatMessage(
      id: json['id'].toString(),
      senderRole: json['senderRole'].toString(),
      senderName: json['senderName'].toString(),
      content: json['content']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
      attachments: rawAttachments,
    );
  }
}
