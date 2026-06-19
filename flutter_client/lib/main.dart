import 'package:flutter/material.dart';

import 'pages/login_page.dart';

void main() {
  runApp(const SmpApp());
}

class SmpApp extends StatelessWidget {
  const SmpApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SMP Chat',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const LoginPage(),
    );
  }
}
