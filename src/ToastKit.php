<?php

namespace Victorycodedev\ToastKit;

use Closure;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

class ToastKit
{
    public function __construct(
        private readonly ?Closure $bridge = null,
        private readonly ToastPresetRegistry $presets = new ToastPresetRegistry(),
    ) {}

    public function make(?string $message = null): PendingToast
    {
        return new PendingToast($this, $message);
    }
    public function success(string $message): PendingToast
    {
        return $this->make($message)->success();
    }
    public function error(string $message): PendingToast
    {
        return $this->make($message)->error();
    }
    public function warning(string $message): PendingToast
    {
        return $this->make($message)->warning();
    }
    public function info(string $message): PendingToast
    {
        return $this->make($message)->info();
    }
    public function neutral(string $message): PendingToast
    {
        return $this->make($message)->neutral();
    }

    public function definePreset(string $name, Closure $preset): void
    {
        $this->presets->define($name, $preset);
    }

    public function preset(string $name): PendingToast
    {
        return $this->presets->apply($name, $this->make());
    }

    public function update(string $id): PendingToastUpdate
    {
        $this->assertIdentifier($id);
        return new PendingToastUpdate($this, $id);
    }

    public function dismiss(string $id): void
    {
        $this->assertIdentifier($id);
        $this->call('ToastKit.Dismiss', ['id' => $id]);
    }

    public function dismissAll(): void
    {
        $this->call('ToastKit.DismissAll', []);
    }

    /** @internal */
    public function show(array $payload): void
    {
        $this->call('ToastKit.Show', $payload);
    }

    /** @internal */
    public function applyUpdate(string $id, array $changes): void
    {
        $this->call('ToastKit.Update', ['id' => $id, 'changes' => $changes]);
    }

    private function call(string $method, array $parameters): mixed
    {
        if ($this->bridge !== null) return ($this->bridge)($method, $parameters);
        if (function_exists('nativephp_call')) {
            return nativephp_call($method, json_encode($parameters, JSON_THROW_ON_ERROR));
        }
        return null;
    }

    /** @internal */
    public function assertIdentifier(string $id): void
    {
        if (trim($id) === '') throw new InvalidToastConfigurationException('Toast IDs must not be empty.');
    }
}
