package de.nogaemer.springhomepage.logger

/**
 * Utility class for formatting console output with ANSI escape codes.
 *
 * Provides methods for colorizing and styling text output in terminal environments
 * that support ANSI escape sequences. Primarily used for enhanced error logging
 * and debugging output visibility.
 *
 * ## ANSI Escape Code Support
 * Works in most modern terminals:
 * - Linux/Unix terminals
 * - macOS Terminal/iTerm2
 * - Windows 10+ Command Prompt/PowerShell
 * - IDE consoles (IntelliJ IDEA, VS Code)
 *
 * ## Formatting Options
 * - **Colors**: 256-color palette + RGB support
 * - **Styles**: Bold, italic, underline, strikethrough
 * - **Inversion**: Swap foreground/background colors
 * - **Reset**: Clear all formatting
 *
 * ## Use Cases
 * - Error highlighting in console logs
 * - Debug output differentiation
 * - Status indication in development
 * - Log level visual distinction
 *
 * ## Production Considerations
 * - ANSI codes may clutter logs in log aggregation systems
 * - Consider disabling colored output for production
 * - Some logging backends strip ANSI codes automatically
 * - File logs may contain escape sequences (harder to read)
 *
 * ## Example Usage
 * ```kotlin
 * val formatter = TextFormatter()
 * println(formatter.error("Error occurred!"))
 * println(formatter.errorInverted("CRITICAL"))
 * println("${formatter.fg(40)}Green text${formatter.RC}")
 * ```
 *
 * @see de.nogaemer.springhomepage.exceptions.ApiExceptionHandler
 */
class TextFormatter {

    /**
     * Reset code - clears all formatting and returns to default colors.
     *
     * Always use after applying formatting to prevent style bleeding.
     * Equivalent to ANSI escape sequence `\u001b[0m`.
     */
    val RC = "\u001b[0m" // Reset foreground and background colors. --> $RC
    
    /**
     * Reverse/invert code - swaps foreground and background colors.
     *
     * Useful for highlighting important text by inverting colors.
     * Equivalent to ANSI escape sequence `\u001b[7m`.
     */
    val R = "\u001b[7m"  // Invert foreground to background. --> $R
    
    /**
     * Underline code - adds underline to text.
     *
     * Equivalent to ANSI escape sequence `\u001b[4m`.
     */
    val U = "\u001b[4m"  // Underline. --> $U
    
    /**
     * Bold code - makes text bold/bright.
     *
     * Equivalent to ANSI escape sequence `\u001b[1m`.
     */
    val B = "\u001b[1m"  // Bold. --> $B
    
    /**
     * Italic code - makes text italic.
     *
     * Note: Not all terminals support italic text.
     * Equivalent to ANSI escape sequence `\u001b[3m`.
     */
    val I = "\u001b[3m"  // Italic. --> $I
    
    /**
     * Strikethrough code - adds line through text.
     *
     * Note: Not all terminals support strikethrough.
     * Equivalent to ANSI escape sequence `\u001b[9m`.
     */
    val S = "\u001b[9m"  // Strikethrough the text. --> $S

    /**
     * Sets foreground color using 256-color palette.
     *
     * Provides access to 256 color options (0-255):
     * - 0-15: Standard colors (black, red, green, yellow, blue, magenta, cyan, white)
     * - 16-231: 216 color cube (6x6x6 RGB)
     * - 232-255: 24 grayscale shades
     *
     * ## Example
     * ```kotlin
     * "${fg(196)}Red text${RC}"  // Bright red
     * "${fg(40)}Green text${RC}"  // Green
     * ```
     *
     * @param n Color number (0-255)
     * @return ANSI escape sequence for foreground color
     */
    fun fg(n: Int) = "\u001b[38;5;${n}m" // Set a foreground color. --> ${fg(40)} //Sets a green color.
    
    /**
     * Sets background color using 256-color palette.
     *
     * Same color range as [fg] but applies to background instead.
     *
     * ## Example
     * ```kotlin
     * "${bg(196)}Red background${RC}"
     * ```
     *
     * @param n Color number (0-255)
     * @return ANSI escape sequence for background color
     */
    fun bg(n: Int) = "\u001b[48;5;${n}m" // Set a background color. --> ${bg(196)} //Sets a red color.

    /**
     * Sets foreground color using RGB values.
     *
     * Provides true color support for terminals that support it.
     * Allows access to 16+ million colors.
     *
     * ## Example
     * ```kotlin
     * "${rgb(255, 0, 0)}Red text${RC}"
     * "${rgb(0, 255, 0)}Green text${RC}"
     * ```
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return ANSI escape sequence for RGB foreground color
     */
    fun rgb(r: Int, g: Int, b: Int) =
        "\u001b[38;2;$r;$g;${b}m" // Set a RGB foreground color. --> ${rgbfg(0,255,0)} //Sets a green color.

    /**
     * Sets background color using RGB values.
     *
     * Same as [rgb] but applies to background.
     *
     * ## Example
     * ```kotlin
     * "${rgbbg(255, 0, 0)}Red background${RC}"
     * ```
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return ANSI escape sequence for RGB background color
     */
    fun rgbbg(r: Int, g: Int, b: Int) =
        "\u001b[48;2;$r;$g;${b}m" // Set a RGB background color. --> ${rgbbg(255,0,0)} //Sets a red color.

    /**
     * Formats message as inverted error with red foreground on light background.
     *
     * Creates high-visibility error header style:
     * - Red text (RGB: 219, 50, 50)
     * - Light gray background (RGB: 230, 230, 230)
     * - Bold and inverted
     *
     * ## Use Case
     * Exception class names in error output for immediate recognition.
     *
     * ## Example Output
     * ```
     *  IdNotFoundException  (white on red background)
     * ```
     *
     * @param message Text to format as inverted error
     * @return Formatted string with ANSI codes
     */
    fun errorInverted(message: String) = "${rgb(219, 50, 50)}$R$B${rgbbg(230, 230, 230)} $message $RC "

    /**
     * Formats message as bold red error text.
     *
     * Creates standard error text style:
     * - Red text (RGB: 219, 50, 50)
     * - Bold
     * - No background color
     *
     * ## Use Case
     * Error messages and descriptions.
     *
     * @param message Text to format as error
     * @return Formatted string with ANSI codes
     */
    fun error(message: String) = "${rgb(219, 50, 50)}$B $message $RC"
    
    /**
     * Formats message as sub-error with bold red text on light background.
     *
     * Creates sub-section error style for structured error output:
     * - Red text (RGB: 219, 50, 50)
     * - Light gray background (RGB: 230, 230, 230)
     * - Bold
     *
     * ## Use Case
     * Error section headers like "Exception:", "Stacktrace:", "Cause:".
     *
     * @param message Text to format as sub-error header
     * @return Formatted string with ANSI codes
     */
    fun subErrorInverted(message: String) = "${rgb(219, 50, 50)}$B${rgbbg(230, 230, 230)} $message $RC "
}
