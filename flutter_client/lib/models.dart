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

class DoctorOption {
  const DoctorOption({
    required this.id,
    required this.name,
    required this.specialization,
  });

  final String id;
  final String name;
  final String specialization;

  factory DoctorOption.fromJson(Map<String, dynamic> json) {
    return DoctorOption(
      id: json['id'].toString(),
      name: json['name']?.toString() ?? '',
      specialization: json['specialization']?.toString() ?? '',
    );
  }
}

class DoctorServiceOption {
  const DoctorServiceOption({
    required this.id,
    required this.name,
    required this.durationMinutes,
    required this.price,
  });

  final String id;
  final String name;
  final int durationMinutes;
  final String price;

  factory DoctorServiceOption.fromJson(Map<String, dynamic> json) {
    return DoctorServiceOption(
      id: json['id'].toString(),
      name: json['name']?.toString() ?? '',
      durationMinutes: (json['durationMinutes'] as num?)?.toInt() ?? 0,
      price: json['price']?.toString() ?? '',
    );
  }
}

class FreeAppointmentSlot {
  const FreeAppointmentSlot({
    required this.startTime,
    required this.endTime,
  });

  final String startTime;
  final String endTime;

  factory FreeAppointmentSlot.fromJson(Map<String, dynamic> json) {
    return FreeAppointmentSlot(
      startTime: json['startTime']?.toString() ?? '',
      endTime: json['endTime']?.toString() ?? '',
    );
  }
}

class AppointmentSummary {
  const AppointmentSummary({
    required this.id,
    required this.doctorId,
    required this.doctorName,
    required this.patientId,
    required this.patientName,
    required this.serviceId,
    required this.serviceName,
    required this.appointmentTime,
    required this.endTime,
    required this.status,
    required this.reason,
    required this.notes,
  });

  final String id;
  final String doctorId;
  final String doctorName;
  final String patientId;
  final String patientName;
  final String serviceId;
  final String serviceName;
  final String appointmentTime;
  final String endTime;
  final String? status;
  final String? reason;
  final String? notes;

  factory AppointmentSummary.fromJson(Map<String, dynamic> json) {
    return AppointmentSummary(
      id: json['id'].toString(),
      doctorId: json['doctorId']?.toString() ?? '',
      doctorName: json['doctorName']?.toString() ?? '',
      patientId: json['patientId']?.toString() ?? '',
      patientName: json['patientName']?.toString() ?? '',
      serviceId: json['serviceId']?.toString() ?? '',
      serviceName: json['serviceName']?.toString() ?? '',
      appointmentTime: json['appointmentTime']?.toString() ?? '',
      endTime: json['endTime']?.toString() ?? '',
      status: json['status']?.toString(),
      reason: json['reason']?.toString(),
      notes: json['notes']?.toString(),
    );
  }
}

class DoctorPrescription {
  const DoctorPrescription({
    required this.id,
    required this.doctorId,
    required this.doctorName,
    required this.patientId,
    required this.patientName,
    required this.appointmentId,
    required this.ticketId,
    required this.qrCodeImageUrl,
    required this.medicationName,
    required this.dosage,
    required this.instructions,
    required this.fulfillmentStatus,
    required this.issuedAt,
    required this.validUntil,
  });

  final String id;
  final String doctorId;
  final String doctorName;
  final String patientId;
  final String patientName;
  final String? appointmentId;
  final String ticketId;
  final String? qrCodeImageUrl;
  final String medicationName;
  final String dosage;
  final String instructions;
  final String? fulfillmentStatus;
  final String issuedAt;
  final String? validUntil;

  factory DoctorPrescription.fromJson(Map<String, dynamic> json) {
    return DoctorPrescription(
      id: json['id'].toString(),
      doctorId: json['doctorId']?.toString() ?? '',
      doctorName: json['doctorName']?.toString() ?? '',
      patientId: json['patientId']?.toString() ?? '',
      patientName: json['patientName']?.toString() ?? '',
      appointmentId: json['appointmentId']?.toString(),
      ticketId: json['ticketId']?.toString() ?? '',
      qrCodeImageUrl: json['qrCodeImageUrl']?.toString(),
      medicationName: json['medicationName']?.toString() ?? '',
      dosage: json['dosage']?.toString() ?? '',
      instructions: json['instructions']?.toString() ?? '',
      fulfillmentStatus: json['fulfillmentStatus']?.toString(),
      issuedAt: json['issuedAt']?.toString() ?? '',
      validUntil: json['validUntil']?.toString(),
    );
  }
}

class DoctorAvailability {
  const DoctorAvailability({
    required this.id,
    required this.dayOfWeek,
    required this.startTime,
    required this.endTime,
    required this.slotDurationMinutes,
    required this.active,
  });

  final String id;
  final String dayOfWeek;
  final String startTime;
  final String endTime;
  final int slotDurationMinutes;
  final bool active;

  factory DoctorAvailability.fromJson(Map<String, dynamic> json) {
    return DoctorAvailability(
      id: json['id'].toString(),
      dayOfWeek: json['dayOfWeek']?.toString() ?? '',
      startTime: json['startTime']?.toString() ?? '',
      endTime: json['endTime']?.toString() ?? '',
      slotDurationMinutes: (json['slotDurationMinutes'] as num?)?.toInt() ?? 0,
      active: json['active'] == true,
    );
  }
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

class PatientPrescription {
  const PatientPrescription({
    required this.id,
    required this.doctorId,
    required this.doctorName,
    required this.patientId,
    required this.patientName,
    required this.appointmentId,
    required this.ticketId,
    required this.ticketToken,
    required this.qrCodeImageUrl,
    required this.ticketDetailsUrl,
    required this.medicationName,
    required this.dosage,
    required this.instructions,
    required this.fulfillmentStatus,
    required this.dispensedByPharmacistId,
    required this.dispensedByPharmacistName,
    required this.dispensedAt,
    required this.dispenseNote,
    required this.issuedAt,
    required this.validUntil,
  });

  final String id;
  final String doctorId;
  final String doctorName;
  final String patientId;
  final String patientName;
  final String? appointmentId;
  final String ticketId;
  final String? ticketToken;
  final String? qrCodeImageUrl;
  final String? ticketDetailsUrl;
  final String medicationName;
  final String dosage;
  final String instructions;
  final String? fulfillmentStatus;
  final String? dispensedByPharmacistId;
  final String? dispensedByPharmacistName;
  final String? dispensedAt;
  final String? dispenseNote;
  final String issuedAt;
  final String? validUntil;

  factory PatientPrescription.fromJson(Map<String, dynamic> json) {
    return PatientPrescription(
      id: json['id'].toString(),
      doctorId: json['doctorId']?.toString() ?? '',
      doctorName: json['doctorName']?.toString() ?? '',
      patientId: json['patientId']?.toString() ?? '',
      patientName: json['patientName']?.toString() ?? '',
      appointmentId: json['appointmentId']?.toString(),
      ticketId: json['ticketId']?.toString() ?? '',
      ticketToken: json['ticketToken']?.toString(),
      qrCodeImageUrl: json['qrCodeImageUrl']?.toString(),
      ticketDetailsUrl: json['ticketDetailsUrl']?.toString(),
      medicationName: json['medicationName']?.toString() ?? '',
      dosage: json['dosage']?.toString() ?? '',
      instructions: json['instructions']?.toString() ?? '',
      fulfillmentStatus: json['fulfillmentStatus']?.toString(),
      dispensedByPharmacistId: json['dispensedByPharmacistId']?.toString(),
      dispensedByPharmacistName: json['dispensedByPharmacistName']?.toString(),
      dispensedAt: json['dispensedAt']?.toString(),
      dispenseNote: json['dispenseNote']?.toString(),
      issuedAt: json['issuedAt']?.toString() ?? '',
      validUntil: json['validUntil']?.toString(),
    );
  }
}

class PatientDocument {
  const PatientDocument({
    required this.id,
    required this.fileName,
    required this.contentType,
    required this.fileSize,
    required this.description,
    required this.uploadedAt,
  });

  final String id;
  final String fileName;
  final String contentType;
  final int fileSize;
  final String? description;
  final String uploadedAt;

  factory PatientDocument.fromJson(Map<String, dynamic> json) {
    return PatientDocument(
      id: json['id'].toString(),
      fileName: json['fileName']?.toString() ?? 'document',
      contentType: json['contentType']?.toString() ?? 'application/octet-stream',
      fileSize: (json['fileSize'] as num?)?.toInt() ?? 0,
      description: json['description']?.toString(),
      uploadedAt: json['uploadedAt']?.toString() ?? '',
    );
  }
}

class PatientHealthStatus {
  const PatientHealthStatus({
    required this.id,
    required this.patientId,
    required this.status,
    required this.notes,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String patientId;
  final String status;
  final String? notes;
  final String createdAt;
  final String updatedAt;

  factory PatientHealthStatus.fromJson(Map<String, dynamic> json) {
    return PatientHealthStatus(
      id: json['id'].toString(),
      patientId: json['patientId']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      notes: json['notes']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
      updatedAt: json['updatedAt']?.toString() ?? '',
    );
  }
}

class AllergyOption {
  const AllergyOption({
    required this.id,
    required this.name,
    required this.description,
  });

  final String id;
  final String name;
  final String description;

  factory AllergyOption.fromJson(Map<String, dynamic> json) {
    return AllergyOption(
      id: json['id'].toString(),
      name: json['name']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
    );
  }
}
