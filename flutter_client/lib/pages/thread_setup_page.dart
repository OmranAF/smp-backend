import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import '../session.dart';
import 'chat_page.dart';
import 'login_page.dart';

class ThreadSetupPage extends StatefulWidget {
  const ThreadSetupPage({required this.session, super.key});

  final SessionContext session;

  @override
  State<ThreadSetupPage> createState() => _ThreadSetupPageState();
}

class _ThreadSetupPageState extends State<ThreadSetupPage> {
  List<ConversationPartner> _partners = const [];
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadPartners();
  }

  @override
  void dispose() {
    super.dispose();
  }

  Future<void> _loadPartners() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final api = ApiClient(widget.session.baseUrl);
      final partners = await api.getConversationPartners(
        authHeader: widget.session.basicAuthHeader,
      );

      if (!mounted) return;
      setState(() => _partners = partners);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _openThread(ConversationPartner partner) async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final api = ApiClient(widget.session.baseUrl);
      final thread = await api.createOrGetThread(
        authHeader: widget.session.basicAuthHeader,
        doctorId: widget.session.user.role == AppRole.doctor
            ? widget.session.user.id
            : partner.id,
        patientId: widget.session.user.role == AppRole.patient
            ? widget.session.user.id
            : partner.id,
      );

      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => ChatPage(
            session: widget.session,
            thread: thread,
          ),
        ),
      );
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  void _logout() {
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute<void>(builder: (_) => const LoginPage()),
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.session.user;
    final counterpartLabel = user.role == AppRole.doctor ? 'Patients' : 'Doctors';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Open Conversation'),
        actions: [
          TextButton(onPressed: _logout, child: const Text('Logout')),
        ],
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 700),
          child: Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Logged in as ${user.name} (${user.role.name})',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  Text('Your ID: ${user.id}'),
                  const SizedBox(height: 4),
                  Text('Backend: ${widget.session.baseUrl}'),
                  const SizedBox(height: 16),
                  Text(
                    'Available $counterpartLabel',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 12),
                  if (_loading && _partners.isEmpty)
                    const Center(child: CircularProgressIndicator())
                  else if (_partners.isEmpty)
                    Text(
                      'No available $counterpartLabel found yet. A doctor-patient appointment must exist first.',
                    )
                  else
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxHeight: 320),
                      child: ListView.separated(
                        shrinkWrap: true,
                        itemCount: _partners.length,
                        separatorBuilder: (context, index) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final partner = _partners[index];
                          return ListTile(
                            title: Text(partner.name),
                            subtitle: Text('${partner.subtitle} - ${partner.email}'),
                            trailing: ElevatedButton(
                              onPressed: _loading ? null : () => _openThread(partner),
                              child: const Text('Open Chat'),
                            ),
                          );
                        },
                      ),
                    ),
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Text(_error!, style: const TextStyle(color: Colors.red)),
                    ),
                  const SizedBox(height: 16),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton.icon(
                      onPressed: _loading ? null : _loadPartners,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Refresh Partners'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
