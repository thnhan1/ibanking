package vn.id.nhanbe.ibanking.dto;

import java.util.List;

public record TransactionHistoryResponse(List<TransactionHistoryItem> items, int page, int size, long totalElements, long totalPages) {
}
