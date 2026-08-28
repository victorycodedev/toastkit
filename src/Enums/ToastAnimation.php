<?php

namespace Victorycodedev\ToastKit\Enums;

enum ToastAnimation: string
{
    case Fade = 'fade';
    case Slide = 'slide';
    case Scale = 'scale';
    case Spring = 'spring';
    case Snap = 'snap';
    case Pop = 'pop';
    case Reveal = 'reveal';
    case Bounce = 'bounce';
}