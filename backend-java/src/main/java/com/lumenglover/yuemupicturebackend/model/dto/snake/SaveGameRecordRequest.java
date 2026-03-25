package com.lumenglover.yuemupicturebackend.model.dto.snake;

import lombok.Data;

@Data
public class SaveGameRecordRequest {
    private Integer score;
    private Integer foodCount;
    private Integer gameTime;
    private Integer snakeLength;
    private Integer gameMode;
} 