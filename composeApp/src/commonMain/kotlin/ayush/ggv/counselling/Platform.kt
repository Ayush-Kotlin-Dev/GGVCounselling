package ayush.ggv.counselling

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform