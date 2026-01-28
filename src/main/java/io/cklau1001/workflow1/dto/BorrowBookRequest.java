package io.cklau1001.workflow1.dto;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BorrowBookRequest {

    @NonNull
    protected String name;

    @NonNull
    protected String bookName;

}
