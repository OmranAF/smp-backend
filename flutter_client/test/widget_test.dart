import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:smp_flutter_client/main.dart';

void main() {
  testWidgets('shows the login page on launch', (WidgetTester tester) async {
    await tester.pumpWidget(const SmpApp());

    expect(find.text('SMP Login'), findsOneWidget);
    expect(find.text('Doctor-Patient Portal'), findsOneWidget);
    expect(find.text('Login'), findsOneWidget);
  });
}
