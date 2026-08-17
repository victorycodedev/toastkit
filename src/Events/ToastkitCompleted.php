<?php

namespace Victorycodedev\Toastkit\Events;

use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class ToastkitCompleted
{
    use Dispatchable, SerializesModels;

    public function __construct(
        public string $result,
        public ?string $id = null
    ) {}
}