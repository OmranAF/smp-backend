import 'dart:async';

import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import '../session.dart';
import 'login_page.dart';

class DoctorPage extends StatefulWidget {
  const DoctorPage({required this.session, super.key});

  final SessionContext session;

  @override
  State<DoctorPage> createState() => _DoctorPageState();
}

class _DoctorPageState extends State<DoctorPage> {
  static const List<String> _days = <String>[
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ];

  static const List<int> _slotDurations = <int>[15, 20, 30, 45, 60];

  final TextEditingController _chatComposerController = TextEditingController();
  final TextEditingController _prescriptionMedicationController = TextEditingController();
  final TextEditingController _prescriptionDosageController = TextEditingController();
  final TextEditingController _prescriptionInstructionsController = TextEditingController();

  List<DoctorAvailability> _availabilities = const [];
  List<AppointmentSummary> _appointments = const [];
  List<DoctorPrescription> _prescriptions = const [];
  List<ConversationPartner> _partners = const [];
  Map<String, ConversationThread> _threadByPartnerId = <String, ConversationThread>{};
  Map<String, DateTime> _lastSeenByThreadId = <String, DateTime>{};
  Set<String> _threadsWithNewPatientMessages = <String>{};
  List<ChatMessage> _activeMessages = const [];

  ConversationPartner? _selectedPartner;
  ConversationThread? _selectedThread;

  bool _loadingInitial = true;
  bool _loadingAvailabilities = false;
  bool _loadingAppointments = false;
  bool _loadingChats = false;
  bool _loadingPrescriptions = false;
  bool _addingAvailability = false;
  bool _creatingPrescription = false;
  bool _sendingChatMessage = false;
  String? _error;

  String _selectedDay = _days.first;
  int _selectedSlotDuration = _slotDurations[2];
  TimeOfDay _selectedStart = const TimeOfDay(hour: 9, minute: 0);
  TimeOfDay _selectedEnd = const TimeOfDay(hour: 12, minute: 0);
  DateTime? _prescriptionValidUntil;
  String? _selectedPrescriptionPatientId;
  String? _selectedPrescriptionAppointmentId;

  Timer? _chatPollTimer;

  ApiClient get _api => ApiClient(widget.session.baseUrl);

  @override
  void initState() {
    super.initState();
    _loadDashboard();
  }

  @override
  void dispose() {
    _chatPollTimer?.cancel();
    _chatComposerController.dispose();
    _prescriptionMedicationController.dispose();
    _prescriptionDosageController.dispose();
    _prescriptionInstructionsController.dispose();
    super.dispose();
  }

  Future<void> _loadDashboard() async {
    setState(() {
      _loadingInitial = true;
      _error = null;
    });

    try {
      await Future.wait<void>([
        _loadAvailabilities(silent: true),
        _loadAppointments(silent: true),
        _loadPrescriptions(silent: true),
        _bootstrapChats(silent: true),
      ]);
      _startChatPolling();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _loadingInitial = false);
      }
    }
  }

  Future<void> _loadAvailabilities({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingAvailabilities = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getDoctorAvailabilities(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.id,
      );
      if (!mounted) return;
      setState(() => _availabilities = items);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingAvailabilities = false);
      }
    }
  }

  Future<void> _loadAppointments({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingAppointments = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getDoctorAppointments(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.id,
      );
      if (!mounted) return;
      setState(() => _appointments = items);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingAppointments = false);
      }
    }
  }

  Future<void> _loadPrescriptions({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingPrescriptions = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getDoctorPrescriptions(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.id,
      );
      if (!mounted) return;
      setState(() => _prescriptions = items);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingPrescriptions = false);
      }
    }
  }

  Future<void> _bootstrapChats({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingChats = true;
        _error = null;
      });
    }

    try {
      final partners = await _api.getConversationPartners(
        authHeader: widget.session.basicAuthHeader,
      );

      final Map<String, ConversationThread> threads = <String, ConversationThread>{};
      final Map<String, DateTime> seenByThread = Map<String, DateTime>.from(_lastSeenByThreadId);
      final Set<String> newMessageThreads = <String>{};
      List<ChatMessage> selectedThreadMessages = _activeMessages;

      for (final partner in partners) {
        final thread = await _api.createOrGetThread(
          authHeader: widget.session.basicAuthHeader,
          doctorId: widget.session.user.id,
          patientId: partner.id,
        );
        threads[partner.id] = thread;

        final messages = await _api.getMessages(
          authHeader: widget.session.basicAuthHeader,
          conversationId: thread.id,
        );

        final latestMessageAt = _latestMessageTimestamp(messages);
        final baseline = seenByThread[thread.id];

        if (baseline == null) {
          if (latestMessageAt != null) {
            seenByThread[thread.id] = latestMessageAt;
          }
        } else {
          final hasNewPatientMessage = messages.any((message) {
            final messageTime = _parseMessageTimestamp(message.createdAt);
            if (messageTime == null) return false;
            return message.senderRole.toUpperCase() == 'PATIENT' && messageTime.isAfter(baseline);
          });

          if (hasNewPatientMessage) {
            newMessageThreads.add(thread.id);
          }
        }

        if (_selectedThread?.id == thread.id) {
          selectedThreadMessages = messages;
        }
      }

      if (!mounted) return;
      setState(() {
        _partners = partners;
        _threadByPartnerId = threads;
        _lastSeenByThreadId = seenByThread;
        _threadsWithNewPatientMessages = newMessageThreads;
        _activeMessages = selectedThreadMessages;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingChats = false);
      }
    }
  }

  void _startChatPolling() {
    _chatPollTimer?.cancel();
    _chatPollTimer = Timer.periodic(const Duration(seconds: 6), (_) {
      _bootstrapChats(silent: true);
    });
  }

  Future<void> _openPartnerChat(ConversationPartner partner) async {
    final thread = _threadByPartnerId[partner.id];
    if (thread == null) return;

    setState(() {
      _selectedPartner = partner;
      _selectedThread = thread;
      _loadingChats = true;
      _error = null;
    });

    try {
      final messages = await _api.getMessages(
        authHeader: widget.session.basicAuthHeader,
        conversationId: thread.id,
      );

      if (!mounted) return;
      setState(() {
        _activeMessages = messages;
        _markThreadSeen(thread.id, messages);
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _loadingChats = false);
      }
    }
  }

  Future<void> _sendChatMessage() async {
    final thread = _selectedThread;
    if (thread == null) return;

    final content = _chatComposerController.text.trim();
    if (content.isEmpty) return;

    setState(() {
      _sendingChatMessage = true;
      _error = null;
    });

    try {
      await _api.sendTextMessage(
        authHeader: widget.session.basicAuthHeader,
        conversationId: thread.id,
        content: content,
      );

      final messages = await _api.getMessages(
        authHeader: widget.session.basicAuthHeader,
        conversationId: thread.id,
      );

      if (!mounted) return;
      setState(() {
        _chatComposerController.clear();
        _activeMessages = messages;
        _markThreadSeen(thread.id, messages);
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _sendingChatMessage = false);
      }
    }
  }

  void _markThreadSeen(String threadId, List<ChatMessage> messages) {
    final latest = _latestMessageTimestamp(messages);
    if (latest != null) {
      _lastSeenByThreadId[threadId] = latest;
    }
    _threadsWithNewPatientMessages.remove(threadId);
  }

  Future<void> _addAvailability() async {
    final startMinutes = _selectedStart.hour * 60 + _selectedStart.minute;
    final endMinutes = _selectedEnd.hour * 60 + _selectedEnd.minute;
    if (startMinutes >= endMinutes) {
      _showSnack('Start time must be before end time');
      return;
    }

    setState(() {
      _addingAvailability = true;
      _error = null;
    });

    try {
      await _api.createDoctorAvailability(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.id,
        dayOfWeek: _selectedDay,
        startTime: _formatTimeForApi(_selectedStart),
        endTime: _formatTimeForApi(_selectedEnd),
        slotDurationMinutes: _selectedSlotDuration,
      );
      await _loadAvailabilities(silent: false);
      _showSnack('Availability added');
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _addingAvailability = false);
      }
    }
  }

  Future<void> _deleteAvailability(String id) async {
    try {
      await _api.deleteDoctorAvailability(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.id,
        availabilityId: id,
      );
      await _loadAvailabilities(silent: false);
      _showSnack('Availability deleted');
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    }
  }

  Future<void> _pickPrescriptionValidUntil() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _prescriptionValidUntil ?? now,
      firstDate: now.subtract(const Duration(days: 1)),
      lastDate: now.add(const Duration(days: 365)),
    );
    if (picked == null) return;
    setState(() => _prescriptionValidUntil = picked);
  }

  Future<void> _createPrescription() async {
    final medication = _prescriptionMedicationController.text.trim();
    final dosage = _prescriptionDosageController.text.trim();
    final instructions = _prescriptionInstructionsController.text.trim();
    final patientId = _selectedPrescriptionPatientId;

    if (patientId == null || patientId.isEmpty) {
      _showSnack('Select a patient first');
      return;
    }
    if (medication.isEmpty || dosage.isEmpty || instructions.isEmpty) {
      _showSnack('Medication, dosage, and instructions are required');
      return;
    }

    setState(() {
      _creatingPrescription = true;
      _error = null;
    });

    try {
      await _api.createDoctorPrescription(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.id,
        patientId: patientId,
        appointmentId: _selectedPrescriptionAppointmentId,
        medicationName: medication,
        dosage: dosage,
        instructions: instructions,
        validUntil: _prescriptionValidUntil,
      );

      _prescriptionMedicationController.clear();
      _prescriptionDosageController.clear();
      _prescriptionInstructionsController.clear();
      _selectedPrescriptionAppointmentId = null;

      await _loadPrescriptions(silent: false);
      _showSnack('Digital prescription created with QR code');
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _creatingPrescription = false);
      }
    }
  }

  Future<void> _pickStartTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _selectedStart,
    );
    if (picked == null) return;
    setState(() => _selectedStart = picked);
  }

  Future<void> _pickEndTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _selectedEnd,
    );
    if (picked == null) return;
    setState(() => _selectedEnd = picked);
  }

  void _logout() {
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute<void>(builder: (_) => const LoginPage()),
      (route) => false,
    );
  }

  DateTime? _parseMessageTimestamp(String raw) {
    try {
      return DateTime.parse(raw).toLocal();
    } catch (_) {
      return null;
    }
  }

  DateTime? _latestMessageTimestamp(List<ChatMessage> messages) {
    DateTime? latest;
    for (final message in messages) {
      final timestamp = _parseMessageTimestamp(message.createdAt);
      if (timestamp == null) continue;
      if (latest == null || timestamp.isAfter(latest)) {
        latest = timestamp;
      }
    }
    return latest;
  }

  String _formatDateTime(String raw) {
    try {
      final parsed = DateTime.parse(raw).toLocal();
      final year = parsed.year.toString();
      final month = parsed.month.toString().padLeft(2, '0');
      final day = parsed.day.toString().padLeft(2, '0');
      final hour = parsed.hour.toString().padLeft(2, '0');
      final minute = parsed.minute.toString().padLeft(2, '0');
      return '$year-$month-$day $hour:$minute';
    } catch (_) {
      return raw;
    }
  }

  String _formatDateOnly(DateTime date) {
    final year = date.year.toString();
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  List<AppointmentSummary> _appointmentsForSelectedPatient() {
    final patientId = _selectedPrescriptionPatientId;
    if (patientId == null || patientId.isEmpty) {
      return const [];
    }
    return _appointments.where((a) => a.patientId == patientId).toList();
  }

  String _formatTimeForApi(TimeOfDay value) {
    final hour = value.hour.toString().padLeft(2, '0');
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute:00';
  }

  String _formatTimeForDisplay(String raw) {
    if (raw.length >= 5) {
      return raw.substring(0, 5);
    }
    return raw;
  }

  String _formatMessagePreview(ChatMessage message) {
    final content = (message.content ?? '').trim();
    if (content.isNotEmpty) {
      return content;
    }
    if (message.attachments.isNotEmpty) {
      return '${message.attachments.length} attachment(s)';
    }
    return 'New message';
  }

  void _showSnack(String text) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }

  @override
  Widget build(BuildContext context) {
    if (_loadingInitial) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Doctor Hub'),
        actions: [
          IconButton(
            onPressed: _loadDashboard,
            icon: const Icon(Icons.refresh),
          ),
          TextButton(onPressed: _logout, child: const Text('Logout')),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 1200),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_error != null)
                  Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.red.withValues(alpha: 0.1),
                      border: Border.all(color: Colors.red.withValues(alpha: 0.3)),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(_error!, style: const TextStyle(color: Colors.red)),
                  ),
                _buildDoctorHeader(),
                const SizedBox(height: 16),
                _buildAvailabilitySection(),
                const SizedBox(height: 16),
                _buildAppointmentsSection(),
                const SizedBox(height: 16),
                _buildPrescriptionSection(),
                const SizedBox(height: 16),
                _buildChatSection(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildDoctorHeader() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          colors: [Color(0xFF0B2447), Color(0xFF1E4FA1), Color(0xFF38A3A5)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Doctor dashboard',
            style: TextStyle(color: Colors.white70, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 8),
          Text(
            'Welcome, ${widget.session.user.name}',
            style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          Text(
            'Manage free appointments, review booked patients, and chat with notifications for new patient messages.',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.9)),
          ),
          const SizedBox(height: 8),
          Text(
            'Doctor ID: ${widget.session.user.id}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.75), fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildAvailabilitySection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Free appointment settings',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                SizedBox(
                  width: 190,
                  child: DropdownButtonFormField<String>(
                    initialValue: _selectedDay,
                    decoration: const InputDecoration(labelText: 'Day'),
                    items: _days
                        .map((day) => DropdownMenuItem<String>(value: day, child: Text(day)))
                        .toList(),
                    onChanged: (value) {
                      if (value == null) return;
                      setState(() => _selectedDay = value);
                    },
                  ),
                ),
                SizedBox(
                  width: 180,
                  child: OutlinedButton.icon(
                    onPressed: _pickStartTime,
                    icon: const Icon(Icons.schedule),
                    label: Text('Start ${_selectedStart.format(context)}'),
                  ),
                ),
                SizedBox(
                  width: 180,
                  child: OutlinedButton.icon(
                    onPressed: _pickEndTime,
                    icon: const Icon(Icons.schedule_send),
                    label: Text('End ${_selectedEnd.format(context)}'),
                  ),
                ),
                SizedBox(
                  width: 210,
                  child: DropdownButtonFormField<int>(
                    initialValue: _selectedSlotDuration,
                    decoration: const InputDecoration(labelText: 'Slot duration'),
                    items: _slotDurations
                        .map((minutes) => DropdownMenuItem<int>(
                              value: minutes,
                              child: Text('$minutes minutes'),
                            ))
                        .toList(),
                    onChanged: (value) {
                      if (value == null) return;
                      setState(() => _selectedSlotDuration = value);
                    },
                  ),
                ),
                FilledButton.icon(
                  onPressed: _addingAvailability ? null : _addAvailability,
                  icon: _addingAvailability
                      ? const SizedBox(
                          height: 16,
                          width: 16,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.add),
                  label: Text(_addingAvailability ? 'Adding...' : 'Add free appointment'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Current availability slots',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
                TextButton.icon(
                  onPressed: _loadingAvailabilities ? null : () => _loadAvailabilities(silent: false),
                  icon: const Icon(Icons.refresh),
                  label: const Text('Refresh'),
                ),
              ],
            ),
            if (_loadingAvailabilities && _availabilities.isEmpty)
              const Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_availabilities.isEmpty)
              const Padding(
                padding: EdgeInsets.all(8),
                child: Text('No availability defined yet.'),
              )
            else
              Column(
                children: _availabilities
                    .map(
                      (item) => ListTile(
                        contentPadding: EdgeInsets.zero,
                        title: Text('${item.dayOfWeek} • ${_formatTimeForDisplay(item.startTime)}-${_formatTimeForDisplay(item.endTime)}'),
                        subtitle: Text('Every ${item.slotDurationMinutes} minutes • ${item.active ? 'Active' : 'Inactive'}'),
                        trailing: IconButton(
                          onPressed: () => _deleteAvailability(item.id),
                          icon: const Icon(Icons.delete_outline),
                        ),
                      ),
                    )
                    .toList(),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppointmentsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Booked appointments',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                  ),
                ),
                TextButton.icon(
                  onPressed: _loadingAppointments ? null : () => _loadAppointments(silent: false),
                  icon: const Icon(Icons.refresh),
                  label: const Text('Refresh'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_loadingAppointments && _appointments.isEmpty)
              const Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_appointments.isEmpty)
              const Text('No patient has booked an appointment yet.')
            else
              Column(
                children: _appointments
                    .map(
                      (appointment) => Container(
                        width: double.infinity,
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFE5E7EB)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    appointment.patientName,
                                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                                  ),
                                ),
                                _SmallPill(label: appointment.status ?? 'UNKNOWN'),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text('${appointment.serviceName} • ${_formatDateTime(appointment.appointmentTime)}'),
                            if ((appointment.reason ?? '').isNotEmpty)
                              Padding(
                                padding: const EdgeInsets.only(top: 4),
                                child: Text('Reason: ${appointment.reason}'),
                              ),
                          ],
                        ),
                      ),
                    )
                    .toList(),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildChatSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Chat service',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                  ),
                ),
                TextButton.icon(
                  onPressed: _loadingChats ? null : () => _bootstrapChats(silent: false),
                  icon: const Icon(Icons.refresh),
                  label: const Text('Refresh chats'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            const Text('A badge appears when a patient sends a new message.'),
            const SizedBox(height: 12),
            LayoutBuilder(
              builder: (context, constraints) {
                final bool stacked = constraints.maxWidth < 900;
                final double leftWidth = stacked ? constraints.maxWidth : 320;
                final double rightWidth = stacked ? constraints.maxWidth : constraints.maxWidth - leftWidth - 16;

                return Wrap(
                  spacing: 16,
                  runSpacing: 16,
                  children: [
                    SizedBox(width: leftWidth, child: _buildPartnerList()),
                    SizedBox(width: rightWidth, child: _buildActiveConversation()),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPrescriptionSection() {
    final appointmentsForPatient = _appointmentsForSelectedPatient();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Digital prescriptions (QR)',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                  ),
                ),
                TextButton.icon(
                  onPressed: _loadingPrescriptions ? null : () => _loadPrescriptions(silent: false),
                  icon: const Icon(Icons.refresh),
                  label: const Text('Refresh'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            const Text('Create a digital prescription for a booked patient. The patient app shows the same QR code.'),
            const SizedBox(height: 12),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                SizedBox(
                  width: 280,
                  child: DropdownButtonFormField<String>(
                    initialValue: _selectedPrescriptionPatientId,
                    decoration: const InputDecoration(labelText: 'Patient'),
                    items: _appointments
                        .map((a) => a.patientId)
                        .toSet()
                        .map((patientId) {
                          final sample = _appointments.firstWhere((a) => a.patientId == patientId);
                          return DropdownMenuItem<String>(
                            value: patientId,
                            child: Text(sample.patientName),
                          );
                        })
                        .toList(),
                    onChanged: (value) {
                      setState(() {
                        _selectedPrescriptionPatientId = value;
                        _selectedPrescriptionAppointmentId = null;
                      });
                    },
                  ),
                ),
                SizedBox(
                  width: 320,
                  child: DropdownButtonFormField<String>(
                    initialValue: _selectedPrescriptionAppointmentId,
                    decoration: const InputDecoration(labelText: 'Appointment (optional)'),
                    items: appointmentsForPatient
                        .map(
                          (a) => DropdownMenuItem<String>(
                            value: a.id,
                            child: Text('${_formatDateTime(a.appointmentTime)} • ${a.serviceName}'),
                          ),
                        )
                        .toList(),
                    onChanged: (value) {
                      setState(() => _selectedPrescriptionAppointmentId = value);
                    },
                  ),
                ),
                OutlinedButton.icon(
                  onPressed: _pickPrescriptionValidUntil,
                  icon: const Icon(Icons.event),
                  label: Text(
                    _prescriptionValidUntil == null
                        ? 'Set valid until'
                        : 'Valid until ${_formatDateOnly(_prescriptionValidUntil!)}',
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _prescriptionMedicationController,
              decoration: const InputDecoration(
                labelText: 'Medication name',
                hintText: 'e.g. Ibuprofen 400mg',
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _prescriptionDosageController,
              decoration: const InputDecoration(
                labelText: 'Dosage',
                hintText: 'e.g. 1 tablet twice daily',
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _prescriptionInstructionsController,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(
                labelText: 'Instructions',
                hintText: 'How patient should take medication',
              ),
            ),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: _creatingPrescription ? null : _createPrescription,
              icon: _creatingPrescription
                  ? const SizedBox(
                      height: 16,
                      width: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.qr_code_2),
              label: Text(_creatingPrescription ? 'Creating...' : 'Create digital prescription'),
            ),
            const SizedBox(height: 16),
            if (_loadingPrescriptions && _prescriptions.isEmpty)
              const Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_prescriptions.isEmpty)
              const Text('No prescriptions created yet.')
            else
              Column(
                children: _prescriptions
                    .map(
                      (prescription) => Container(
                        width: double.infinity,
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFE5E7EB)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${prescription.patientName} • ${prescription.medicationName}',
                              style: const TextStyle(fontWeight: FontWeight.w700),
                            ),
                            const SizedBox(height: 4),
                            Text('${prescription.dosage} • ${prescription.instructions}'),
                            const SizedBox(height: 4),
                            Text('Issued: ${_formatDateTime(prescription.issuedAt)}'),
                            if ((prescription.validUntil ?? '').isNotEmpty)
                              Text('Valid until: ${prescription.validUntil}'),
                            if ((prescription.qrCodeImageUrl ?? '').isNotEmpty) ...[
                              const SizedBox(height: 8),
                              ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: Image.network(
                                  prescription.qrCodeImageUrl!,
                                  width: 120,
                                  height: 120,
                                  fit: BoxFit.cover,
                                  errorBuilder: (_, __, ___) => const Text('QR preview unavailable'),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    )
                    .toList(),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildPartnerList() {
    if (_loadingChats && _partners.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(20),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (_partners.isEmpty) {
      return const Text('No patient chats available yet. A booked relationship is required first.');
    }

    return ConstrainedBox(
      constraints: const BoxConstraints(maxHeight: 430),
      child: ListView.separated(
        shrinkWrap: true,
        itemCount: _partners.length,
        separatorBuilder: (context, index) => const Divider(height: 1),
        itemBuilder: (context, index) {
          final partner = _partners[index];
          final thread = _threadByPartnerId[partner.id];
          final selected = _selectedPartner?.id == partner.id;
          final hasNew = thread != null && _threadsWithNewPatientMessages.contains(thread.id);

          return ListTile(
            selected: selected,
            title: Row(
              children: [
                Expanded(child: Text(partner.name)),
                if (hasNew)
                  const _SmallPill(label: 'NEW'),
              ],
            ),
            subtitle: Text(partner.email),
            onTap: () => _openPartnerChat(partner),
          );
        },
      ),
    );
  }

  Widget _buildActiveConversation() {
    if (_selectedThread == null || _selectedPartner == null) {
      return const Text('Select a patient on the left to open a one-to-one chat.');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Conversation with ${_selectedPartner!.name}',
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 8),
        if (_activeMessages.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 12),
            child: Text('No messages yet.'),
          )
        else
          ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 320),
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: _activeMessages.length,
              separatorBuilder: (context, index) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final message = _activeMessages[index];
                final isMine = message.senderRole.toUpperCase() == 'DOCTOR';
                return Align(
                  alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
                  child: Container(
                    constraints: const BoxConstraints(maxWidth: 620),
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: isMine
                          ? Theme.of(context).colorScheme.primaryContainer
                          : Theme.of(context).colorScheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '${message.senderName} (${message.senderRole})',
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
                        ),
                        if ((message.content ?? '').trim().isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 4),
                            child: Text(message.content!),
                          ),
                        if (message.attachments.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 6),
                            child: Text(_formatMessagePreview(message), style: const TextStyle(fontSize: 12)),
                          ),
                        Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(_formatDateTime(message.createdAt), style: const TextStyle(fontSize: 11)),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _chatComposerController,
                minLines: 1,
                maxLines: 3,
                decoration: const InputDecoration(
                  hintText: 'Type message to patient',
                  border: OutlineInputBorder(),
                ),
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              onPressed: _sendingChatMessage ? null : _sendChatMessage,
              child: _sendingChatMessage
                  ? const SizedBox(
                      height: 16,
                      width: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Text('Send'),
            ),
          ],
        ),
      ],
    );
  }
}

class _SmallPill extends StatelessWidget {
  const _SmallPill({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFE0EEFF),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: Color(0xFF1D4ED8)),
      ),
    );
  }
}
