<?php

namespace Victorycodedev\ToastKit\Enums;

enum ToastVariant: string
{
    case Success = 'success';
    case Error = 'error';
    case Warning = 'warning';
    case Info = 'info';
    case Neutral = 'neutral';
}
