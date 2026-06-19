import 'dart:async';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import '../session.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({
    required this.session,
    required this.thread,
    super.key,
  });

  final SessionContext session;
  final ConversationThread thread;

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  List<ChatMessage> _messages = const [];
  bool _loading = false;
  String? _error;
  Timer? _pollTimer;

  ApiClient get _api => ApiClient(widget.session.baseUrl);

  @override
  void initState() {
    super.initState();
    _loadMessages(showLoader: true);
    _pollTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _loadMessages(showLoader: false);
    });
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _loadMessages({required bool showLoader}) async {
    if (showLoader) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }

    try {
      final items = await _api.getMessages(
        authHeader: widget.session.basicAuthHeader,
        conversationId: widget.thread.id,
      );

      if (!mounted) return;
      setState(() => _messages = items);
      _scrollToBottom();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (showLoader && mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _sendText() async {
    final content = _messageController.text.trim();
    if (content.isEmpty) return;

    setState(() => _loading = true);

    try {
      await _api.sendTextMessage(
        authHeader: widget.session.basicAuthHeader,
        conversationId: widget.thread.id,
        content: content,
      );
      _messageController.clear();
      await _loadMessages(showLoader: false);
    } catch (e) {
      _showSnack(e.toString());
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _sendAttachment() async {
    final result = await FilePicker.platform.pickFiles(withData: true, allowMultiple: false);
    if (result == null || result.files.isEmpty) return;

    final picked = result.files.single;
    if (picked.bytes == null) {
      _showSnack('Could not read file bytes');
      return;
    }

    setState(() => _loading = true);

    try {
      await _api.sendAttachment(
        authHeader: widget.session.basicAuthHeader,
        conversationId: widget.thread.id,
        bytes: picked.bytes!,
        filename: picked.name,
        content: _messageController.text.trim().isEmpty ? null : _messageController.text.trim(),
      );
      _messageController.clear();
      await _loadMessages(showLoader: false);
    } catch (e) {
      _showSnack(e.toString());
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _saveToPatientProfile(String attachmentId) async {
    if (widget.session.user.role != AppRole.doctor) return;

    setState(() => _loading = true);

    try {
      await _api.saveAttachmentToPatientProfile(
        authHeader: widget.session.basicAuthHeader,
        conversationId: widget.thread.id,
        attachmentId: attachmentId,
      );
      await _loadMessages(showLoader: false);
      _showSnack('Attachment saved to patient profile');
    } catch (e) {
      _showSnack(e.toString());
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Conversation'),
        actions: [
          IconButton(
            onPressed: _loading ? null : () => _loadMessages(showLoader: true),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Column(
        children: [
          Container(
            width: double.infinity,
            color: Theme.of(context).colorScheme.surfaceContainer,
            padding: const EdgeInsets.all(12),
            child: Text(
              'Thread: ${widget.thread.id} | Role: ${widget.session.user.role.name}',
              style: const TextStyle(fontSize: 12),
            ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.all(8),
              child: Text(_error!, style: const TextStyle(color: Colors.red)),
            ),
          Expanded(
            child: _loading && _messages.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _buildMessages(),
          ),
          _buildComposer(),
        ],
      ),
    );
  }

  Widget _buildMessages() {
    if (_messages.isEmpty) {
      return const Center(child: Text('No messages yet.'));
    }

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.all(12),
      itemCount: _messages.length,
      itemBuilder: (context, index) {
        final message = _messages[index];
        final isMine = message.senderRole.toUpperCase() == widget.session.user.role.name.toUpperCase();

        return Align(
          alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: Card(
              color: isMine
                  ? Theme.of(context).colorScheme.primaryContainer
                  : Theme.of(context).colorScheme.surfaceContainerHighest,
              child: Padding(
                padding: const EdgeInsets.all(10),
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
                    if (message.attachments.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: message.attachments
                              .map((a) => Row(
                                    children: [
                                      Expanded(
                                        child: Text(
                                          '${a.fileName} (${a.fileSize} bytes)',
                                          style: const TextStyle(fontSize: 12),
                                        ),
                                      ),
                                      if (widget.session.user.role == AppRole.doctor)
                                        TextButton(
                                          onPressed: a.savedToPatientProfile
                                              ? null
                                              : () => _saveToPatientProfile(a.id),
                                          child: Text(
                                            a.savedToPatientProfile
                                                ? 'Saved'
                                                : 'Save to profile',
                                          ),
                                        ),
                                    ],
                                  ))
                              .toList(),
                        ),
                      ),
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(
                        message.createdAt,
                        style: const TextStyle(fontSize: 11),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildComposer() {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _messageController,
              minLines: 1,
              maxLines: 3,
              decoration: const InputDecoration(
                hintText: 'Type message',
                border: OutlineInputBorder(),
              ),
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            onPressed: _loading ? null : _sendAttachment,
            icon: const Icon(Icons.attach_file),
            tooltip: 'Attach file',
          ),
          IconButton(
            onPressed: _loading ? null : _sendText,
            icon: const Icon(Icons.send),
            tooltip: 'Send',
          ),
        ],
      ),
    );
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    });
  }

  void _showSnack(String text) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }
}
