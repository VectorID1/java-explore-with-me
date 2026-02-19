package ru.practicum.ewm.main.model;

public enum RequestStatus {
    PENDING,                                   // в ожидании
    CONFIRMED,                        // подтверждена
    REJECTED,                 // отклонена
    CANCELED          // отменена пользователем
}