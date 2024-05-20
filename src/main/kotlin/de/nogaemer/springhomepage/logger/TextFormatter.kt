package de.nogaemer.springhomepage.logger

class TextFormatter {

    val RC = "\u001b[0m" // Reset foreground and background colors. --> $RC
    val R = "\u001b[7m"  // Invert foreground to background. --> $R
    val U = "\u001b[4m"  // Underline. --> $U
    val B = "\u001b[1m"  // Bold. --> $B
    val I = "\u001b[3m"  // Italic. --> $I
    val S = "\u001b[9m"  // Strikethrough the text. --> $S

    // 256 Colors
    fun fg(n: Int) = "\u001b[38;5;${n}m" // Set a foreground color. --> ${fg(40)} //Sets a green color.
    fun bg(n: Int) = "\u001b[48;5;${n}m" // Set a background color. --> ${bg(196)} //Sets a red color.

    // RGB Colors
    fun rgb(r: Int, g: Int, b: Int) =
        "\u001b[38;2;$r;$g;${b}m" // Set a RGB foreground color. --> ${rgbfg(0,255,0)} //Sets a green color.

    fun rgbbg(r: Int, g: Int, b: Int) =
        "\u001b[48;2;$r;$g;${b}m" // Set a RGB background color. --> ${rgbbg(255,0,0)} //Sets a red color.

    fun errorInverted(message: String) = "${rgb(219, 50, 50)}$R$B${rgbbg(230, 230, 230)} $message $RC "

    fun error(message: String) = "${rgb(219, 50, 50)}$B $message $RC"
    fun subErrorInverted(message: String) = "${rgb(219, 50, 50)}$B${rgbbg(230, 230, 230)} $message $RC "
}
