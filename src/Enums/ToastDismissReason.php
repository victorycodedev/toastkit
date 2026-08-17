<?php

namespace Victorycodedev\ToastKit\Enums;

enum ToastDismissReason: string
{
    case Timeout = 'timeout';
    case Swipe = 'swipe';
    case Programmatic = 'programmatic';
    case Action = 'action';
    case Replaced = 'replaced';
}
