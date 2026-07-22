package gg.sona.eos

/** Platform creation flags. */
public object EosPlatformFlags {
    /** The SDK is being loaded in a game editor (Unity/Unreal play-in-editor). */
    public const val LoadingInEditor: Int = 0x00001

    /** Skip initialization of the overlay used by the in-app purchase flow. */
    public const val DisableOverlay: Int = 0x00002

    /** Skip initialization of the social overlay. */
    public const val DisableSocialOverlay: Int = 0x00004

    /** Reserved. */
    public const val Reserved1: Int = 0x00008

    /** Enable experimental D3D9 overlay support on Windows. */
    public const val WindowsEnableOverlayD3D9: Int = 0x00010

    /** Enable experimental D3D10 overlay support on Windows. */
    public const val WindowsEnableOverlayD3D10: Int = 0x00020

    /** Enable experimental OpenGL overlay support on Windows. */
    public const val WindowsEnableOverlayOpenGL: Int = 0x00040

    /** Enable automatic unloading of the overlay module on consoles. */
    public const val ConsoleEnableOverlayAutomaticUnloading: Int = 0x00080

    /** Enable verbose debug logging related to the overlay on consoles. */
    public const val EnableOverlayDebugLogging: Int = 0x00100
}
