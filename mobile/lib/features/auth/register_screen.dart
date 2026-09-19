import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/strings.dart';
import 'auth_view_model.dart';
import 'validators.dart';
import 'widgets/password_field.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _submit(AuthViewModel viewModel) async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final success = await viewModel.register(
      _emailController.text.trim(),
      _passwordController.text,
    );
    if (!mounted) return;
    if (success) {
      // The auth gate behind this route already switched to the tasks
      // screen; popping reveals it.
      Navigator.of(context).pop();
    } else if (viewModel.errorMessage != null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(viewModel.errorMessage!)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<AuthViewModel>();
    return Scaffold(
      appBar: AppBar(title: const Text(Strings.createAccount)),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 400),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TextFormField(
                      controller: _emailController,
                      keyboardType: TextInputType.emailAddress,
                      textInputAction: TextInputAction.next,
                      autofillHints: const [AutofillHints.email],
                      decoration: const InputDecoration(
                        labelText: Strings.email,
                        border: OutlineInputBorder(),
                      ),
                      validator: AuthValidators.email,
                    ),
                    const SizedBox(height: 16),
                    PasswordField(
                      controller: _passwordController,
                      labelText: Strings.password,
                      textInputAction: TextInputAction.next,
                      validator: AuthValidators.password,
                    ),
                    const SizedBox(height: 16),
                    PasswordField(
                      controller: _confirmController,
                      labelText: Strings.confirmPassword,
                      textInputAction: TextInputAction.done,
                      validator: AuthValidators.confirmPassword(
                        _passwordController,
                      ),
                      onFieldSubmitted: (_) => _submit(viewModel),
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: viewModel.isSubmitting
                          ? null
                          : () => _submit(viewModel),
                      child: viewModel.isSubmitting
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text(Strings.register),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
