package ru.practicum.ewm.main.model;

public enum EventState {
    PENDING,                                // Ожидает модерации
    PUBLISHED,                      // Опубликовано
    CANCELED                // Отменено
}