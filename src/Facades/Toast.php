<?php

namespace Victorycodedev\ToastKit\Facades;

use Illuminate\Support\Facades\Facade;
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\PendingToastUpdate;

/**
 * @method static PendingToast make(?string $message = null)
 * @method static PendingToast success(string $message)
 * @method static PendingToast error(string $message)
 * @method static PendingToast warning(string $message)
 * @method static PendingToast info(string $message)
 * @method static PendingToast neutral(string $message)
 * @method static void definePreset(string $name, \Closure $preset)
 * @method static PendingToast preset(string $name)
 * @method static PendingToastUpdate update(string $id)
 * @method static void dismiss(string $id)
 * @method static void dismissAll()
 */
class Toast extends Facade
{
    protected static function getFacadeAccessor(): string { return 'toastkit'; }
}
