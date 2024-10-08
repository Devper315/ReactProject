package com.example.backend.entity.chat;

import com.example.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "message")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageCustom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    Conversation conversation;

    @ManyToOne
    User sender;

    String content;
    LocalDateTime time;
    MessageStatus status;
}
