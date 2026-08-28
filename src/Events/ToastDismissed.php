<?php

namespace Victorycodedev\ToastKit\Events;

use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;
use Victorycodedev\ToastKit\Enums\ToastDismissReason;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

class ToastDismissed
{
    use Dispatchable, SerializesModels;

    public function __construct(public string $toastId, public string $reason)
    {
        if (ToastDismissReason::tryFrom($reason) === null) {
            throw new InvalidToastConfigurationException("Invalid toast dismissal reason: {$reason}.");
        }
    }
}
