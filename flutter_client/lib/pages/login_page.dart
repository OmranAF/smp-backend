import 'dart:convert';

import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import '../session.dart';
import 'thread_setup_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final TextEditingController _baseUrlController =
      TextEditingController(text: 'http://localhost:8080');
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  AppRole _role = AppRole.doctor;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _baseUrlController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final baseUrl = _baseUrlController.text.trim();
    final email = _emailController.text.trim();
    final password = _passwordController.text;

    if (baseUrl.isEmpty || email.isEmpty || password.isEmpty) {
      setState(() => _error = 'Please fill all fields');
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final api = ApiClient(baseUrl);
      final user = switch (_role) {
        AppRole.doctor => await api.loginDoctor(email, password),
        AppRole.patient => await api.loginPatient(email, password),
      };

      final basic = 'Basic ${base64Encode(utf8.encode('$email:$password'))}';
      final session = SessionContext(
        baseUrl: baseUrl,
        user: user,
        basicAuthHeader: basic,
      );

      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => ThreadSetupPage(session: session),
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('SMP Login')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 560),
          child: Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    'Doctor-Patient Chat',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _baseUrlController,
                    decoration: const InputDecoration(
                      labelText: 'Backend Base URL',
                      helperText: 'Web: http://localhost:8080 | Android emulator: http://10.0.2.2:8080',
                    ),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<AppRole>(
                    initialValue: _role,
                    decoration: const InputDecoration(labelText: 'Role'),
                    items: const [
                      DropdownMenuItem(value: AppRole.doctor, child: Text('Doctor')),
                      DropdownMenuItem(value: AppRole.patient, child: Text('Patient')),
                    ],
                    onChanged: _loading
                        ? null
                        : (value) {
                            if (value != null) {
                              setState(() => _role = value);
                            }
                          },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _emailController,
                    decoration: const InputDecoration(labelText: 'Email'),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _passwordController,
                    obscureText: true,
                    decoration: const InputDecoration(labelText: 'Password'),
                  ),
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Text(_error!, style: const TextStyle(color: Colors.red)),
                    ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: _loading ? null : _submit,
                      child: _loading
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('Login'),
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
