import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_exception.dart';

DioException _errorWithResponse({
  required int statusCode,
  required Object? data,
}) {
  final requestOptions = RequestOptions(path: '/tasks');
  return DioException(
    requestOptions: requestOptions,
    response: Response(
      requestOptions: requestOptions,
      statusCode: statusCode,
      data: data,
    ),
    type: DioExceptionType.badResponse,
  );
}

void main() {
  group('ApiException.fromDioError', () {
    test('parses a problem+json body with field errors', () {
      final error = _errorWithResponse(
        statusCode: 400,
        data: {
          'type': 'about:blank',
          'title': 'Bad Request',
          'status': 400,
          'detail': 'Request validation failed.',
          'code': 'VALIDATION_FAILED',
          'errors': [
            {'field': 'title', 'message': 'must not be blank'},
          ],
        },
      );

      final exception = ApiException.fromDioError(error);

      expect(exception.statusCode, 400);
      expect(exception.code, 'VALIDATION_FAILED');
      expect(exception.detail, 'Request validation failed.');
      expect(exception.fieldErrors, hasLength(1));
      expect(exception.messageForField('title'), 'must not be blank');
      expect(exception.messageForField('unknown'), isNull);
    });

    test('parses a problem+json body with no field errors', () {
      final error = _errorWithResponse(
        statusCode: 401,
        data: {
          'code': 'INVALID_CREDENTIALS',
          'detail': 'Invalid email or password.',
        },
      );

      final exception = ApiException.fromDioError(error);

      expect(exception.code, 'INVALID_CREDENTIALS');
      expect(exception.detail, 'Invalid email or password.');
      expect(exception.fieldErrors, isEmpty);
    });

    test('falls back to a generic message when the body is not JSON', () {
      final error = _errorWithResponse(statusCode: 500, data: 'oops');

      final exception = ApiException.fromDioError(error);

      expect(exception.statusCode, 500);
      expect(exception.code, isNull);
      expect(exception.detail, isNotEmpty);
    });

    test('falls back to a network message when there is no response', () {
      final requestOptions = RequestOptions(path: '/tasks');
      final error = DioException(
        requestOptions: requestOptions,
        type: DioExceptionType.connectionError,
      );

      final exception = ApiException.fromDioError(error);

      expect(exception.statusCode, isNull);
      expect(exception.detail, contains('Could not reach the server'));
    });
  });
}
