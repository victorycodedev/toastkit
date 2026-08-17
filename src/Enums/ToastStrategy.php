<?php

namespace Victorycodedev\ToastKit\Enums;

enum ToastStrategy: string
{
    case Queue = 'queue';
    case Stack = 'stack';
}
