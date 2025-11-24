package com.example.splendar.domain.game

data class BoardData(
    val nobleTiles: List<StaticNoble>,
    val level3Cards: List<StaticCard>,
    val level2Cards: List<StaticCard>,
    val level1Cards: List<StaticCard>,
    val tokens: List<Tokens> ,
    val playerState: List<PlayerState>
)
