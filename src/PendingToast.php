<?php

namespace Victorycodedev\ToastKit;

use Victorycodedev\ToastKit\Concerns\ConfiguresToast;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

class PendingToast
{
    use ConfiguresToast;

    private string $id;
    private array $options = [];

    public function __construct(private readonly ToastKit $toastKit, ?string $message = null)
    {
        $this->id = self::uuid();
        if ($message !== null) $this->message($message);
    }

    public function id(string $id): static
    {
        $this->toastKit->assertIdentifier($id);
        $this->id = $id;
        return $this;
    }

    public function unique(string $key): static
    {
        $this->toastKit->assertUniqueKey($key);
        $this->options['unique_key'] = $key;
        return $this;
    }

    public function show(): string
    {
        if (! isset($this->options['message'])) throw new InvalidToastConfigurationException('A toast message is required before show().');

        $variant = $this->options['variant'] ?? 'neutral';
        $payload = array_replace_recursive(
            $this->defaults(),
            $this->variantDefaults($variant),
            $this->options,
            ['id' => $this->id, 'contract_version' => 1]
        );
        return $this->toastKit->show($payload) ?? $this->id;
    }

    /** @internal */
    public function payload(): array
    {
        if (! isset($this->options['message'])) throw new InvalidToastConfigurationException('A toast message is required before show().');
        $variant = $this->options['variant'] ?? 'neutral';
        return array_replace_recursive($this->defaults(), $this->variantDefaults($variant), $this->options, [
            'id' => $this->id,
            'contract_version' => 1,
        ]);
    }

    protected function put(string $key, mixed $value): static
    {
        $this->options[$key] = $value;
        return $this;
    }

    protected function putStyle(string $key, mixed $value): static
    {
        $this->options['style'][$key] = $value;
        return $this;
    }

    protected function putTypography(string $group, array $values): static
    {
        $this->options[$group] = array_merge($this->options[$group] ?? [], $values);
        return $this;
    }

    private function defaults(): array
    {
        return [
            'title' => null,
            'variant' => 'neutral',
            'position' => 'bottom',
            'duration' => 3000,
            'persistent' => false,
            'animation' => 'scale',
            'direction' => 'auto',
            'loading' => false,
            'swipe_to_dismiss' => true,
            'dismissible' => false,
            'style' => ['corner_radius' => 16, 'padding' => 16, 'shadow' => true],
            'strategy' => 'queue',
            'max_visible' => 3,
            'overflow_behavior' => 'queue',
        ];
    }

    private function variantDefaults(string $variant): array
    {
        return match ($variant) {
            'success' => ['icon' => ['name' => 'check'], 'style' => ['background' => '#166534', 'foreground' => '#FFFFFF', 'icon_color' => '#86EFAC']],
            'error' => ['icon' => ['name' => 'error'], 'style' => ['background' => '#991B1B', 'foreground' => '#FFFFFF', 'icon_color' => '#FCA5A5']],
            'warning' => ['icon' => ['name' => 'warning'], 'style' => ['background' => '#92400E', 'foreground' => '#FFFFFF', 'icon_color' => '#FDE68A']],
            'info' => ['icon' => ['name' => 'info'], 'style' => ['background' => '#1E40AF', 'foreground' => '#FFFFFF', 'icon_color' => '#93C5FD']],
            default => ['style' => ['background' => '#1F2937', 'foreground' => '#FFFFFF']],
        };
    }

    private static function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        $hex = bin2hex($bytes);

        return sprintf(
            '%s-%s-%s-%s-%s',
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20),
        );
    }
}
