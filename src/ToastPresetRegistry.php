<?php

namespace Victorycodedev\ToastKit;

use Closure;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;
use Victorycodedev\ToastKit\Exceptions\PresetNotFoundException;

class ToastPresetRegistry
{
    /** @var array<string, Closure> */
    private array $presets = [];

    public function define(string $name, Closure $preset): void
    {
        $name = trim($name);
        if ($name === '') throw new InvalidToastConfigurationException('Toast preset names must not be empty.');
        $this->presets[$name] = $preset;
    }

    public function apply(string $name, PendingToast $toast): PendingToast
    {
        $preset = $this->presets[$name] ?? throw new PresetNotFoundException("Toast preset [{$name}] is not defined.");
        $result = $preset($toast);
        if ($result !== null && ! $result instanceof PendingToast) {
            throw new InvalidToastConfigurationException('A toast preset must return its PendingToast builder or null.');
        }
        return $result ?? $toast;
    }
}