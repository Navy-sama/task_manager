class User {
  const User({required this.id, required this.email});

  final int id;
  final String email;

  factory User.fromJson(Map<String, dynamic> json) {
    return User(id: json['id'] as int, email: json['email'] as String);
  }
}
