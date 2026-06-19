import 'models.dart';

class SessionContext {
  const SessionContext({
    required this.baseUrl,
    required this.user,
    required this.basicAuthHeader,
  });

  final String baseUrl;
  final AppUser user;
  final String basicAuthHeader;
}
