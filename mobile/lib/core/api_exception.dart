import 'package:dio/dio.dart';

/// A single field-level validation error, as returned in the `errors` array
/// of a `problem+json` response.
class ApiFieldError {
  const ApiFieldError({required this.field, required this.message});

  final String field;
  final String message;

  factory ApiFieldError.fromJson(Map<String, dynamic> json) {
    return ApiFieldError(
      field: json['field'] as String? ?? '',
      message: json['message'] as String? ?? '',
    );
  }
}

/// App-level representation of a backend error, parsed from the RFC 9457
/// `application/problem+json` body described in `docs/api-contract.md`.
class ApiException implements Exception {
  const ApiException({
    this.statusCode,
    this.code,
    required this.detail,
    this.fieldErrors = const [],
  });

  final int? statusCode;

  /// Stable machine-readable error code (e.g. `VALIDATION_FAILED`).
  final String? code;

  /// Human-readable English sentence, safe to show to the user.
  final String detail;

  final List<ApiFieldError> fieldErrors;

  static const _genericDetail = 'Something went wrong. Please try again.';
  static const _networkDetail =
      'Could not reach the server. Check your connection and try again.';

  factory ApiException.fromDioError(DioException error) {
    if (error.type == DioExceptionType.cancel) {
      return const ApiException(detail: 'Request cancelled.');
    }
    final response = error.response;
    if (response == null) {
      return const ApiException(detail: _networkDetail);
    }
    final data = response.data;
    if (data is Map<String, dynamic>) {
      final rawErrors = data['errors'];
      final fieldErrors = rawErrors is List
          ? rawErrors
                .whereType<Map<String, dynamic>>()
                .map(ApiFieldError.fromJson)
                .toList()
          : const <ApiFieldError>[];
      return ApiException(
        statusCode: response.statusCode,
        code: data['code'] as String?,
        detail: (data['detail'] as String?) ?? _genericDetail,
        fieldErrors: fieldErrors,
      );
    }
    return ApiException(statusCode: response.statusCode, detail: _genericDetail);
  }

  /// The first field error message for [field], if any.
  String? messageForField(String field) {
    for (final error in fieldErrors) {
      if (error.field == field) return error.message;
    }
    return null;
  }

  @override
  String toString() => 'ApiException(code: $code, detail: $detail)';
}
