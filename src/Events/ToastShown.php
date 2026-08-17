<?php

namespace Victorycodedev\ToastKit\Events;

use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class ToastShown
{
    use Dispatchable, SerializesModels;

    public function __construct(public string $toastId) {}
}
