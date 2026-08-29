<?php

namespace Victorycodedev\ToastKit;

use Closure;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;
use Victorycodedev\ToastKit\Exceptions\ToastKitException;

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

    public function updateUnique(string $key): PendingToastUpdate
    {
        $this->assertUniqueKey($key);
        return new PendingToastUpdate($this, $key, usesUniqueKey: true);
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

    public function dismissUnique(string $key): void
    {
        $this->assertUniqueKey($key);
        $this->call('ToastKit.DismissUnique', ['unique_key' => $key]);
    }

    /** @internal */
    public function show(array $payload): ?string
    {
        return $this->responseId($this->call('ToastKit.Show', $payload));
    }

    /** @internal */
    public function applyUpdate(string $id, array $changes): void
    {
        $this->call('ToastKit.Update', ['id' => $id, 'changes' => $changes]);
    }

    /** @internal */
    public function applyUniqueUpdate(string $key, array $changes): ?string
    {
        return $this->responseId($this->call('ToastKit.UpdateUnique', [
            'unique_key' => $key,
            'changes' => $changes,
        ]));
    }

    private function call(string $method, array $parameters): mixed
    {
        if ($this->bridge !== null) return $this->unwrapResponse(($this->bridge)($method, $parameters));
        if (function_exists('nativephp_call')) {
            return $this->unwrapResponse(nativephp_call($method, json_encode($parameters, JSON_THROW_ON_ERROR)));
        }
        return null;
    }

    private function unwrapResponse(mixed $response): mixed
    {
        if (is_string($response)) {
            $decoded = json_decode($response, true);
            if (json_last_error() === JSON_ERROR_NONE) $response = $decoded;
        }
        if (! is_array($response)) return $response;
        if (($response['status'] ?? null) === 'error') {
            throw new ToastKitException($response['message'] ?? 'The native ToastKit operation failed.');
        }
        return $response['data'] ?? $response;
    }

    private function responseId(mixed $response): ?string
    {
        if (! is_array($response)) return null;
        $data = isset($response['data']) && is_array($response['data']) ? $response['data'] : $response;
        return isset($data['id']) && is_string($data['id']) ? $data['id'] : null;
    }

    /** @internal */
    public function assertIdentifier(string $id): void
    {
        if (trim($id) === '') throw new InvalidToastConfigurationException('Toast IDs must not be empty.');
    }

    /** @internal */
    public function assertUniqueKey(string $key): void
    {
        if (trim($key) === '') throw new InvalidToastConfigurationException('Toast unique keys must not be empty.');
    }
}
