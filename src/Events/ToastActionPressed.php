<?php

namespace Victorycodedev\ToastKit\Events;

use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class ToastActionPressed
{
    use Dispatchable, SerializesModels;

    public function __construct(public string $toastId, public string $actionId) {}
}
