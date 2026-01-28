package io.cklau1001.workflow1.dto;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowBookResponse extends BorrowBookRequest {

    @NonNull
    private String requestId;

    public BorrowBookResponse(String requestId, BorrowBookRequest borrowBookRequest) {
        this.requestId = requestId;
        this.bookName = borrowBookRequest.getBookName();
        this.name = borrowBookRequest.getName();
    }
}
