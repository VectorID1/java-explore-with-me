package ru.practicum.ewm.main.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCommentDto {

    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(min = 2, max = 2000, message = "Комментарий должен быть от 2 до 2000 символов")
    private String text;
}
