<?php

namespace Victorycodedev\ToastKit\Events;

use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;
use InvalidArgumentException;
use Victorycodedev\ToastKit\Enums\ToastDismissReason;

class ToastDismissed
{
    use Dispatchable, SerializesModels;

    public function __construct(public string $toastId, public string $reason)
    {
        if (ToastDismissReason::tryFrom($reason) === null) {
            throw new InvalidArgumentException("Invalid toast dismissal reason: {$reason}.");
        }
    }
}
