class PageResponse<T> {
  const PageResponse({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<T> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJsonT,
  ) {
    final rawContent = json['content'] as List<dynamic>? ?? const [];
    return PageResponse(
      content: rawContent
          .map((item) => fromJsonT(item as Map<String, dynamic>))
          .toList(),
      page: json['page'] as int? ?? 0,
      size: json['size'] as int? ?? rawContent.length,
      totalElements: json['totalElements'] as int? ?? rawContent.length,
      totalPages: json['totalPages'] as int? ?? 1,
    );
  }
}
