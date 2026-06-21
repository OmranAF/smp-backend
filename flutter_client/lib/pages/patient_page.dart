import 'dart:async';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import '../session.dart';
import 'login_page.dart';

class PatientPage extends StatefulWidget {
  const PatientPage({required this.session, super.key});

  final SessionContext session;

  @override
  State<PatientPage> createState() => _PatientPageState();
}

class _PatientPageState extends State<PatientPage> {
  final TextEditingController _documentDescriptionController = TextEditingController();
  final TextEditingController _healthStatusController = TextEditingController();
  final TextEditingController _healthNotesController = TextEditingController();
  final TextEditingController _doctorSearchController = TextEditingController();
  final TextEditingController _appointmentReasonController = TextEditingController();
  final TextEditingController _chatMessageController = TextEditingController();

  final GlobalKey _doctorSearchKey = GlobalKey();
  final GlobalKey _chatSectionKey = GlobalKey();

  List<DoctorOption> _doctors = const [];
  List<DoctorOption> _filteredDoctors = const [];
  DoctorOption? _selectedDoctor;
  List<DoctorServiceOption> _doctorServices = const [];
  DoctorServiceOption? _selectedService;
  List<FreeAppointmentSlot> _freeSlots = const [];
  FreeAppointmentSlot? _selectedSlot;
  ConversationThread? _activeThread;
  List<ChatMessage> _chatMessages = const [];

  List<PatientPrescription> _prescriptions = const [];
  List<PatientDocument> _documents = const [];
  List<AllergyOption> _allergies = const [];
  Set<String> _selectedAllergyIds = {};

  bool _loadingInitial = true;
  bool _loadingDoctors = false;
  bool _loadingDoctorContext = false;
  bool _loadingPrescriptions = false;
  bool _loadingDocuments = false;
  bool _loadingAllergies = false;
  bool _savingHealthStatus = false;
  bool _savingAllergies = false;
  bool _uploadingDocuments = false;
  bool _bookingAppointment = false;
  bool _sendingChatMessage = false;
  String? _error;
  String? _statusMessage;
  Timer? _chatTimer;

  ApiClient get _api => ApiClient(widget.session.baseUrl);

  @override
  void initState() {
    super.initState();
    _healthStatusController.text = 'stable';
    _doctorSearchController.addListener(_applyDoctorFilter);
    _loadDashboard();
  }

  @override
  void dispose() {
    _documentDescriptionController.dispose();
    _healthStatusController.dispose();
    _healthNotesController.dispose();
    _doctorSearchController.dispose();
    _appointmentReasonController.dispose();
    _chatMessageController.dispose();
    _chatTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadDashboard() async {
    setState(() {
      _loadingInitial = true;
      _error = null;
      _statusMessage = null;
    });

    try {
      await Future.wait([
        _loadDoctors(silent: true),
        _loadPrescriptions(silent: true),
        _loadDocuments(silent: true),
        _loadAllergies(silent: true),
      ]);
    } finally {
      if (mounted) {
        setState(() => _loadingInitial = false);
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
      final items = await _api.getPatientPrescriptions(
        authHeader: widget.session.basicAuthHeader,
        patientId: widget.session.user.id,
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

  Future<void> _loadDoctors({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingDoctors = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getDoctors(authHeader: widget.session.basicAuthHeader);
      if (!mounted) return;
      setState(() {
        _doctors = items;
        _filteredDoctors = items;
      });
      _applyDoctorFilter();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingDoctors = false);
      }
    }
  }

  void _applyDoctorFilter() {
    final query = _doctorSearchController.text.trim().toLowerCase();
    final next = query.isEmpty
        ? _doctors
        : _doctors.where((doctor) {
            return doctor.name.toLowerCase().contains(query) ||
                doctor.specialization.toLowerCase().contains(query);
          }).toList();

    if (!mounted) return;
    setState(() {
      _filteredDoctors = next;
    });
  }

  Future<void> _selectDoctor(DoctorOption doctor) async {
    setState(() {
      _selectedDoctor = doctor;
      _loadingDoctorContext = true;
      _error = null;
      _selectedService = null;
      _selectedSlot = null;
      _doctorServices = const [];
      _freeSlots = const [];
      _chatMessages = const [];
    });

    try {
      final services = await _api.getDoctorServices(
        authHeader: widget.session.basicAuthHeader,
        doctorId: doctor.id,
      );
      final freeSlots = await _api.getDoctorFreeSlots(
        authHeader: widget.session.basicAuthHeader,
        doctorId: doctor.id,
        date: _appointmentDate,
      );
      final thread = await _api.createOrGetThread(
        authHeader: widget.session.basicAuthHeader,
        doctorId: doctor.id,
        patientId: widget.session.user.id,
      );

      if (!mounted) return;
      setState(() {
        _doctorServices = services;
        _freeSlots = freeSlots;
        _selectedService = services.isNotEmpty ? services.first : null;
        _selectedSlot = freeSlots.isNotEmpty ? freeSlots.first : null;
        _activeThread = thread;
      });

      await _loadChatMessages();
      _startChatPolling();
      _showStatus('Opened ${doctor.name}');
      WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToKey(_chatSectionKey));
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _loadingDoctorContext = false);
      }
    }
  }

  DateTime _appointmentDate = DateTime.now();

  Future<void> _pickAppointmentDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _appointmentDate,
      firstDate: DateTime.now().subtract(const Duration(days: 1)),
      lastDate: DateTime.now().add(const Duration(days: 90)),
    );
    if (picked == null) return;

    setState(() => _appointmentDate = picked);
    if (_selectedDoctor != null) {
      await _reloadFreeSlots();
    }
  }

  Future<void> _reloadFreeSlots() async {
    final doctor = _selectedDoctor;
    if (doctor == null) return;

    try {
      final freeSlots = await _api.getDoctorFreeSlots(
        authHeader: widget.session.basicAuthHeader,
        doctorId: doctor.id,
        date: _appointmentDate,
      );
      if (!mounted) return;
      setState(() {
        _freeSlots = freeSlots;
        _selectedSlot = freeSlots.isNotEmpty ? freeSlots.first : null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    }
  }

  Future<void> _bookAppointment() async {
    final doctor = _selectedDoctor;
    final service = _selectedService;
    final slot = _selectedSlot;
    if (doctor == null || service == null || slot == null) {
      _showStatus('Select a doctor, service, and free slot first');
      return;
    }

    setState(() {
      _bookingAppointment = true;
      _error = null;
    });

    try {
      await _api.createAppointment(
        authHeader: widget.session.basicAuthHeader,
        doctorId: doctor.id,
        patientId: widget.session.user.id,
        serviceId: service.id,
        appointmentTime: DateTime.parse(slot.startTime),
        reason: _appointmentReasonController.text.trim().isEmpty ? null : _appointmentReasonController.text.trim(),
      );
      _showStatus('Appointment booked with ${doctor.name}');
      await _reloadFreeSlots();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _bookingAppointment = false);
      }
    }
  }

  void _startChatPolling() {
    _chatTimer?.cancel();
    _chatTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _loadChatMessages();
    });
  }

  Future<void> _loadChatMessages() async {
    final thread = _activeThread;
    if (thread == null) return;

    try {
      final items = await _api.getMessages(
        authHeader: widget.session.basicAuthHeader,
        conversationId: thread.id,
      );
      if (!mounted) return;
      setState(() => _chatMessages = items);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    }
  }

  Future<void> _sendChatMessage() async {
    final thread = _activeThread;
    final text = _chatMessageController.text.trim();
    if (thread == null || text.isEmpty) return;

    setState(() => _sendingChatMessage = true);

    try {
      await _api.sendTextMessage(
        authHeader: widget.session.basicAuthHeader,
        conversationId: thread.id,
        content: text,
      );
      _chatMessageController.clear();
      await _loadChatMessages();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _sendingChatMessage = false);
      }
    }
  }

  void _scrollToKey(GlobalKey key) {
    final context = key.currentContext;
    if (context == null) return;
    Scrollable.ensureVisible(
      context,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOut,
    );
  }

  Future<void> _loadDocuments({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingDocuments = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getPatientDocuments(
        authHeader: widget.session.basicAuthHeader,
        patientId: widget.session.user.id,
      );
      if (!mounted) return;
      setState(() => _documents = items);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingDocuments = false);
      }
    }
  }

  Future<void> _loadAllergies({required bool silent}) async {
    if (!silent) {
      setState(() {
        _loadingAllergies = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getAllergies(authHeader: widget.session.basicAuthHeader);
      if (!mounted) return;
      setState(() => _allergies = items);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (!silent && mounted) {
        setState(() => _loadingAllergies = false);
      }
    }
  }

  void _jumpToDoctorSearch() {
    _scrollToKey(_doctorSearchKey);
  }

  Future<void> _refreshDocuments() async {
    await _loadDocuments(silent: false);
    _showStatus('Documents refreshed');
  }

  Future<void> _refreshPrescriptions() async {
    await _loadPrescriptions(silent: false);
    _showStatus('Prescriptions refreshed');
  }

  Future<void> _pickAndUploadDocuments() async {
    final result = await FilePicker.platform.pickFiles(withData: true, allowMultiple: true);
    if (result == null || result.files.isEmpty) return;

    setState(() {
      _uploadingDocuments = true;
      _error = null;
    });

    try {
      await _api.uploadPatientDocuments(
        authHeader: widget.session.basicAuthHeader,
        patientId: widget.session.user.id,
        files: result.files,
        description: _documentDescriptionController.text,
      );
      _documentDescriptionController.clear();
      await _loadDocuments(silent: false);
      _showStatus('Documents uploaded');
    } catch (e) {
      if (mounted) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _uploadingDocuments = false);
      }
    }
  }

  Future<void> _saveAllergies() async {
    setState(() {
      _savingAllergies = true;
      _error = null;
    });

    try {
      await _api.setPatientAllergies(
        authHeader: widget.session.basicAuthHeader,
        patientId: widget.session.user.id,
        allergyIds: _selectedAllergyIds.toList(),
      );
      _showStatus('Allergies updated');
    } catch (e) {
      if (mounted) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _savingAllergies = false);
      }
    }
  }

  Future<void> _saveHealthStatus() async {
    final status = _healthStatusController.text.trim();
    if (status.isEmpty) {
      _showStatus('Health status cannot be empty');
      return;
    }

    setState(() {
      _savingHealthStatus = true;
      _error = null;
    });

    try {
      final updated = await _api.updatePatientHealthStatus(
        authHeader: widget.session.basicAuthHeader,
        patientId: widget.session.user.id,
        status: status,
        notes: _healthNotesController.text.trim().isEmpty ? null : _healthNotesController.text.trim(),
      );
      _showStatus('Health status saved: ${updated.status}');
    } catch (e) {
      if (mounted) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _savingHealthStatus = false);
      }
    }
  }

  void _logout() {
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute<void>(builder: (_) => const LoginPage()),
      (route) => false,
    );
  }

  void _showStatus(String message) {
    if (!mounted) return;
    setState(() => _statusMessage = message);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  String _formatDateOnly(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    return '${date.year}-$month-$day';
  }

  String _formatDateTime(String raw) {
    if (raw.isEmpty) return 'n/a';
    try {
      return _formatDateTimeValue(DateTime.parse(raw));
    } catch (_) {
      return raw;
    }
  }

  String _formatDateTimeValue(DateTime dateTime) {
    final day = dateTime.day.toString().padLeft(2, '0');
    final month = dateTime.month.toString().padLeft(2, '0');
    final hour = dateTime.hour.toString().padLeft(2, '0');
    final minute = dateTime.minute.toString().padLeft(2, '0');
    return '${dateTime.year}-$month-$day $hour:$minute';
  }

  String _formatDate(String raw) {
    if (raw.isEmpty) return 'n/a';
    try {
      final parsed = DateTime.parse(raw).toLocal();
      final day = parsed.day.toString().padLeft(2, '0');
      final month = parsed.month.toString().padLeft(2, '0');
      final year = parsed.year.toString();
      final hour = parsed.hour.toString().padLeft(2, '0');
      final minute = parsed.minute.toString().padLeft(2, '0');
      return '$year-$month-$day $hour:$minute';
    } catch (_) {
      return raw;
    }
  }

  String _formatBytes(int bytes) {
    if (bytes < 1024) return '$bytes B';
    final kilobytes = bytes / 1024;
    if (kilobytes < 1024) return '${kilobytes.toStringAsFixed(1)} KB';
    return '${(kilobytes / 1024).toStringAsFixed(1)} MB';
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.session.user;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFF8FAFF), Color(0xFFE8F2FF), Color(0xFFFDFDFD)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: _loadingInitial
              ? const Center(child: CircularProgressIndicator())
              : CustomScrollView(
                  slivers: [
                    SliverAppBar(
                      pinned: true,
                      backgroundColor: Colors.white.withValues(alpha: 0.82),
                      surfaceTintColor: Colors.transparent,
                      title: const Text('Patient Hub'),
                      actions: [
                        IconButton(
                          onPressed: _loadingInitial ? null : _loadDashboard,
                          icon: const Icon(Icons.refresh),
                        ),
                        TextButton(onPressed: _logout, child: const Text('Logout')),
                      ],
                    ),
                    SliverToBoxAdapter(
                      child: Center(
                        child: ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 1180),
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                _buildHero(user),
                                if (_statusMessage != null) ...[
                                  const SizedBox(height: 12),
                                  _statusBanner(_statusMessage!, Colors.teal),
                                ],
                                if (_error != null) ...[
                                  const SizedBox(height: 12),
                                  _statusBanner(_error!, Colors.red),
                                ],
                                const SizedBox(height: 16),
                                _buildDoctorSearchSection(),
                                const SizedBox(height: 16),
                                _buildServiceGrid(),
                                const SizedBox(height: 16),
                                LayoutBuilder(
                                  builder: (context, constraints) {
                                    final wide = constraints.maxWidth >= 920;
                                    final cardWidth = wide ? (constraints.maxWidth - 16) / 2 : constraints.maxWidth;

                                    return Wrap(
                                      spacing: 16,
                                      runSpacing: 16,
                                      children: [
                                        SizedBox(width: cardWidth, child: _buildPrescriptionsCard()),
                                        SizedBox(width: cardWidth, child: _buildDocumentsCard()),
                                        SizedBox(width: cardWidth, child: _buildAllergiesCard()),
                                        SizedBox(width: cardWidth, child: _buildHealthCard()),
                                      ],
                                    );
                                  },
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }

  Widget _buildDoctorSearchSection() {
    return Container(
      key: _doctorSearchKey,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.96),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Find a doctor and book a free slot',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
                ),
              ),
              TextButton.icon(
                onPressed: _loadingDoctors ? null : () => _loadDoctors(silent: false),
                icon: const Icon(Icons.refresh),
                label: const Text('Refresh doctors'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          const Text('Search by name or section, then pick a service and one of the free appointment slots.'),
          const SizedBox(height: 16),
          TextField(
            controller: _doctorSearchController,
            decoration: const InputDecoration(
              labelText: 'Search doctors by section or name',
              prefixIcon: Icon(Icons.search),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Text('Appointment date: ${_formatDateOnly(_appointmentDate)}'),
              const SizedBox(width: 12),
              TextButton.icon(
                onPressed: _pickAppointmentDate,
                icon: const Icon(Icons.event),
                label: const Text('Pick date'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (_loadingDoctors)
            const Center(child: CircularProgressIndicator())
          else if (_filteredDoctors.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Text('No doctors found for that search.'),
            )
          else
            SizedBox(
              height: 220,
              child: ListView.separated(
                itemCount: _filteredDoctors.length,
                separatorBuilder: (context, index) => const Divider(height: 1),
                itemBuilder: (context, index) {
                  final doctor = _filteredDoctors[index];
                  final selected = _selectedDoctor?.id == doctor.id;
                  return ListTile(
                    selected: selected,
                    title: Text(doctor.name),
                    subtitle: Text(doctor.specialization.isEmpty ? 'No section listed' : doctor.specialization),
                    trailing: TextButton(
                      onPressed: _loadingDoctorContext ? null : () => _selectDoctor(doctor),
                      child: Text(selected ? 'Selected' : 'View'),
                    ),
                    onTap: _loadingDoctorContext ? null : () => _selectDoctor(doctor),
                  );
                },
              ),
            ),
          if (_selectedDoctor != null) ...[
            const SizedBox(height: 20),
            _buildSelectedDoctorPanel(),
          ],
        ],
      ),
    );
  }

  Widget _buildSelectedDoctorPanel() {
    final doctor = _selectedDoctor!;

    return Column(
      key: _chatSectionKey,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: const Color(0xFFF8FAFF),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: const Color(0xFFD7E3FF)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      doctor.name,
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                    ),
                  ),
                  _StatusPill(label: doctor.specialization.isEmpty ? 'DOCTOR' : doctor.specialization),
                ],
              ),
              const SizedBox(height: 8),
              Text('Book a service and open the embedded 1:1 chat with this doctor.'),
              if (_loadingDoctorContext) ...[
                const SizedBox(height: 12),
                const LinearProgressIndicator(),
              ],
              const SizedBox(height: 16),
              Text(
                'Services',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              if (_doctorServices.isEmpty)
                const Text('No services listed for this doctor yet.')
              else
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _doctorServices.map((service) {
                    final selected = _selectedService?.id == service.id;
                    return ChoiceChip(
                      label: Text(service.name),
                      selected: selected,
                      onSelected: _loadingDoctorContext
                          ? null
                          : (_) {
                              setState(() => _selectedService = service);
                            },
                    );
                  }).toList(),
                ),
              const SizedBox(height: 16),
              Text(
                'Free appointments for ${_formatDateOnly(_appointmentDate)}',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              if (_freeSlots.isEmpty)
                const Text('No free slots available for this date.')
              else
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _freeSlots.map((slot) {
                    final selected = _selectedSlot?.startTime == slot.startTime;
                    return ChoiceChip(
                      label: Text('${_formatDateTime(slot.startTime)} - ${_formatDateTime(slot.endTime)}'),
                      selected: selected,
                      onSelected: _loadingDoctorContext
                          ? null
                          : (_) {
                              setState(() => _selectedSlot = slot);
                            },
                    );
                  }).toList(),
                ),
              const SizedBox(height: 12),
              TextField(
                controller: _appointmentReasonController,
                decoration: const InputDecoration(
                  labelText: 'Appointment reason',
                  hintText: 'Optional reason for the visit',
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _bookingAppointment ? null : _bookAppointment,
                  icon: _bookingAppointment
                      ? const SizedBox(
                          height: 18,
                          width: 18,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.event_available),
                  label: Text(_bookingAppointment ? 'Booking...' : 'Book appointment'),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _buildEmbeddedChatPanel(),
      ],
    );
  }

  Widget _buildEmbeddedChatPanel() {
    final thread = _activeThread;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Chat',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                ),
              ),
              if (thread != null)
                Text(
                  'Thread ${thread.id}',
                  style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            _selectedDoctor == null
                ? 'Select a doctor to open the 1:1 chat.'
                : 'The chat opens automatically for the selected doctor.',
          ),
          const SizedBox(height: 12),
          if (thread == null)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_chatMessages.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Text('No messages yet. Start the conversation below.'),
            )
          else
            ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 360),
              child: ListView.separated(
                shrinkWrap: true,
                itemCount: _chatMessages.length,
                separatorBuilder: (context, index) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final message = _chatMessages[index];
                  final isMine = message.senderRole.toUpperCase() == widget.session.user.role.name.toUpperCase();
                  return Align(
                    alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
                    child: Container(
                      constraints: const BoxConstraints(maxWidth: 700),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: isMine
                            ? Theme.of(context).colorScheme.primaryContainer
                            : Theme.of(context).colorScheme.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${message.senderName} (${message.senderRole})',
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                          if (message.content != null && message.content!.trim().isNotEmpty)
                            Padding(
                              padding: const EdgeInsets.only(top: 6),
                              child: Text(message.content!),
                            ),
                          Padding(
                            padding: const EdgeInsets.only(top: 6),
                            child: Text(message.createdAt, style: const TextStyle(fontSize: 11)),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _chatMessageController,
                  minLines: 1,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    hintText: 'Type your message',
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton(
                onPressed: _sendingChatMessage ? null : _sendChatMessage,
                child: _sendingChatMessage
                    ? const SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : const Text('Send'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildHero(AppUser user) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0xFF0F172A), Color(0xFF1D4ED8), Color(0xFF38BDF8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(color: Color(0x22000000), blurRadius: 24, offset: Offset(0, 12)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Your care dashboard',
            style: TextStyle(color: Colors.white70, fontSize: 14, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 8),
          Text(
            'Welcome back, ${user.name}',
            style: const TextStyle(color: Colors.white, fontSize: 30, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          Text(
            'Review prescriptions, upload documents, manage allergies, update your health status, and open a doctor chat from one place.',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.9), fontSize: 15, height: 1.4),
          ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: const [
              _HeroChip(label: 'Chat with doctors', icon: Icons.chat_bubble_outline),
              _HeroChip(label: 'Track prescriptions', icon: Icons.receipt_long),
              _HeroChip(label: 'Upload documents', icon: Icons.folder_open),
              _HeroChip(label: 'Update health status', icon: Icons.monitor_heart_outlined),
              _HeroChip(label: 'Manage allergies', icon: Icons.sick_outlined),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'Patient ID: ${user.id}  •  Backend: ${widget.session.baseUrl}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.75), fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildServiceGrid() {
    return LayoutBuilder(
      builder: (context, constraints) {
        final wide = constraints.maxWidth >= 900;
        final cardWidth = wide ? (constraints.maxWidth - 48) / 4 : constraints.maxWidth;

        return Wrap(
          spacing: 16,
          runSpacing: 16,
          children: [
            SizedBox(width: cardWidth, child: _ServiceCard(
              icon: Icons.chat,
              title: 'Find doctors',
              subtitle: 'Search by section and open the embedded chat on the same page.',
              actionLabel: 'Search now',
              onPressed: _jumpToDoctorSearch,
            )),
            SizedBox(width: cardWidth, child: _ServiceCard(
              icon: Icons.receipt_long,
              title: 'Prescriptions',
              subtitle: 'See issued prescriptions and dispensing status.',
              actionLabel: 'Refresh list',
              onPressed: _loadingPrescriptions ? null : _refreshPrescriptions,
            )),
            SizedBox(width: cardWidth, child: _ServiceCard(
              icon: Icons.folder_open,
              title: 'Documents',
              subtitle: 'Upload or review documents stored on your profile.',
              actionLabel: 'Upload files',
              onPressed: _uploadingDocuments ? null : _pickAndUploadDocuments,
            )),
            SizedBox(width: cardWidth, child: _ServiceCard(
              icon: Icons.monitor_heart_outlined,
              title: 'Health status',
              subtitle: 'Share your current status or recovery notes.',
              actionLabel: 'Save status',
              onPressed: _savingHealthStatus ? null : _saveHealthStatus,
            )),
          ],
        );
      },
    );
  }

  Widget _buildPrescriptionsCard() {
    return _SectionCard(
      icon: Icons.receipt_long,
      title: 'My prescriptions',
      subtitle: 'Issued prescriptions with medication, dosage, and fulfillment info.',
      trailing: TextButton.icon(
        onPressed: _loadingPrescriptions ? null : _refreshPrescriptions,
        icon: const Icon(Icons.refresh),
        label: const Text('Refresh'),
      ),
      child: _loadingPrescriptions && _prescriptions.isEmpty
          ? const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            )
          : _prescriptions.isEmpty
              ? const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12),
                  child: Text('No prescriptions yet.'),
                )
              : Column(
                  children: _prescriptions
                      .map(
                        (prescription) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(18),
                              border: Border.all(color: const Color(0xFFE5E7EB)),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Expanded(
                                      child: Text(
                                        prescription.medicationName,
                                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                                      ),
                                    ),
                                    _StatusPill(label: prescription.fulfillmentStatus ?? 'PENDING'),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                Text('${prescription.dosage} • Dr. ${prescription.doctorName}'),
                                const SizedBox(height: 4),
                                Text(prescription.instructions),
                                const SizedBox(height: 8),
                                Text('Issued: ${_formatDate(prescription.issuedAt)}'),
                                if ((prescription.validUntil ?? '').isNotEmpty)
                                  Text('Valid until: ${_formatDate(prescription.validUntil ?? '')}'),
                                if ((prescription.dispensedByPharmacistName ?? '').isNotEmpty) ...[
                                  const SizedBox(height: 4),
                                  Text('Dispensed by: ${prescription.dispensedByPharmacistName}'),
                                ],
                                if ((prescription.dispenseNote ?? '').isNotEmpty) ...[
                                  const SizedBox(height: 4),
                                  Text('Note: ${prescription.dispenseNote}'),
                                ],
                                if ((prescription.qrCodeImageUrl ?? '').isNotEmpty) ...[
                                  const SizedBox(height: 10),
                                  const Text(
                                    'Digital prescription QR',
                                    style: TextStyle(fontWeight: FontWeight.w600),
                                  ),
                                  const SizedBox(height: 6),
                                  ClipRRect(
                                    borderRadius: BorderRadius.circular(8),
                                    child: Image.network(
                                      prescription.qrCodeImageUrl!,
                                      width: 140,
                                      height: 140,
                                      fit: BoxFit.cover,
                                      errorBuilder: (_, __, ___) => const Text('QR code could not be loaded'),
                                    ),
                                  ),
                                ],
                                const SizedBox(height: 8),
                                Text(
                                  'Ticket: ${prescription.ticketId}',
                                  style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        ),
                      )
                      .toList(),
                ),
    );
  }

  Widget _buildDocumentsCard() {
    return _SectionCard(
      icon: Icons.folder_open,
      title: 'My documents',
      subtitle: 'Upload files and keep track of what is stored on your profile.',
      trailing: TextButton.icon(
        onPressed: _loadingDocuments ? null : _refreshDocuments,
        icon: const Icon(Icons.refresh),
        label: const Text('Refresh'),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _documentDescriptionController,
            decoration: const InputDecoration(
              labelText: 'Upload description',
              hintText: 'Optional note for the files you upload',
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _uploadingDocuments ? null : _pickAndUploadDocuments,
              icon: _uploadingDocuments
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.upload_file),
              label: Text(_uploadingDocuments ? 'Uploading...' : 'Upload documents'),
            ),
          ),
          const SizedBox(height: 16),
          if (_loadingDocuments && _documents.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_documents.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Text('No documents uploaded yet.'),
            )
          else
            Column(
              children: _documents
                  .map(
                    (document) => Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: const Color(0xFFE5E7EB)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              document.fileName,
                              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                            ),
                            const SizedBox(height: 4),
                            Text('${document.contentType} • ${_formatBytes(document.fileSize)}'),
                            if ((document.description ?? '').isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Text(document.description!),
                            ],
                            const SizedBox(height: 4),
                            Text('Uploaded: ${_formatDate(document.uploadedAt)}'),
                          ],
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
        ],
      ),
    );
  }

  Widget _buildAllergiesCard() {
    return _SectionCard(
      icon: Icons.sick_outlined,
      title: 'Allergies',
      subtitle: 'Select the allergies you want stored on your profile. Saving replaces the current set.',
      trailing: TextButton.icon(
        onPressed: _loadingAllergies ? null : () => _loadAllergies(silent: false),
        icon: const Icon(Icons.refresh),
        label: const Text('Refresh'),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_loadingAllergies && _allergies.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_allergies.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Text('No allergy catalog entries available.'),
            )
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _allergies
                  .map(
                    (allergy) => FilterChip(
                      label: Text(allergy.name),
                      selected: _selectedAllergyIds.contains(allergy.id),
                      onSelected: _savingAllergies
                          ? null
                          : (selected) {
                              setState(() {
                                if (selected) {
                                  _selectedAllergyIds.add(allergy.id);
                                } else {
                                  _selectedAllergyIds.remove(allergy.id);
                                }
                              });
                            },
                    ),
                  )
                  .toList(),
            ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _savingAllergies ? null : _saveAllergies,
              icon: _savingAllergies
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.save_outlined),
              label: Text(_savingAllergies ? 'Saving...' : 'Save allergies'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHealthCard() {
    return _SectionCard(
      icon: Icons.monitor_heart_outlined,
      title: 'Health status',
      subtitle: 'Share how you feel or add a short recovery note for your care team.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _healthStatusController,
            decoration: const InputDecoration(
              labelText: 'Status',
              hintText: 'e.g. stable, improving, needs attention',
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _healthNotesController,
            minLines: 3,
            maxLines: 5,
            decoration: const InputDecoration(
              labelText: 'Notes',
              hintText: 'Symptoms, recovery notes, or anything the care team should know',
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _savingHealthStatus ? null : _saveHealthStatus,
              icon: _savingHealthStatus
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.send_outlined),
              label: Text(_savingHealthStatus ? 'Saving...' : 'Update health status'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _statusBanner(String message, Color color) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.25)),
      ),
      child: Text(message, style: TextStyle(color: color, fontWeight: FontWeight.w600)),
    );
  }
}

class _HeroChip extends StatelessWidget {
  const _HeroChip({required this.label, required this.icon});

  final String label;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Chip(
      backgroundColor: Colors.white.withValues(alpha: 0.16),
      side: BorderSide(color: Colors.white.withValues(alpha: 0.22)),
      avatar: Icon(icon, size: 18, color: Colors.white),
      label: Text(label, style: const TextStyle(color: Colors.white)),
    );
  }
}

class _ServiceCard extends StatelessWidget {
  const _ServiceCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.actionLabel,
    required this.onPressed,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String actionLabel;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white.withValues(alpha: 0.96),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            CircleAvatar(
              backgroundColor: const Color(0xFFE0EEFF),
              child: Icon(icon, color: const Color(0xFF1D4ED8)),
            ),
            const SizedBox(height: 14),
            Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 6),
            Text(subtitle, style: TextStyle(color: Colors.grey.shade700, height: 1.35)),
            const SizedBox(height: 14),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: onPressed,
                child: Text(actionLabel),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.child,
    this.trailing,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white.withValues(alpha: 0.96),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                CircleAvatar(
                  backgroundColor: const Color(0xFFE0EEFF),
                  child: Icon(icon, color: const Color(0xFF1D4ED8)),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
                      const SizedBox(height: 4),
                      Text(subtitle, style: TextStyle(color: Colors.grey.shade700, height: 1.35)),
                    ],
                  ),
                ),
                if (trailing != null) trailing!,
              ],
            ),
            const SizedBox(height: 16),
            child,
          ],
        ),
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final normalized = label.toUpperCase();
    final background = switch (normalized) {
      'APPROVED' || 'FINISHED' => const Color(0xFFD1FAE5),
      'PENDING' => const Color(0xFFFFEDD5),
      'REJECTED' || 'CANCELLED' => const Color(0xFFFEE2E2),
      _ => const Color(0xFFE0EEFF),
    };
    final foreground = switch (normalized) {
      'APPROVED' || 'FINISHED' => const Color(0xFF065F46),
      'PENDING' => const Color(0xFF9A3412),
      'REJECTED' || 'CANCELLED' => const Color(0xFF991B1B),
      _ => const Color(0xFF1D4ED8),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        normalized,
        style: TextStyle(color: foreground, fontSize: 11, fontWeight: FontWeight.w700),
      ),
    );
  }
}