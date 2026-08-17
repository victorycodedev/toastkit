<?php

namespace Victorycodedev\ToastKit;

use Illuminate\Support\Str;
use InvalidArgumentException;
use Victorycodedev\ToastKit\Concerns\ConfiguresToast;

class PendingToast
{
    use ConfiguresToast;

    private string $id;
    private array $options = [];

    public function __construct(private readonly ToastKit $toastKit, ?string $message = null)
    {
        $this->id = (string) Str::uuid();
        if ($message !== null) $this->message($message);
    }

    public function id(string $id): static
    {
        $this->toastKit->assertIdentifier($id);
        $this->id = $id;
        return $this;
    }

    public function show(): string
    {
        if (! isset($this->options['message'])) throw new InvalidArgumentException('A toast message is required before show().');

        $variant = $this->options['variant'] ?? 'neutral';
        $payload = array_replace_recursive(
            $this->defaults(),
            $this->variantDefaults($variant),
            $this->options,
            ['id' => $this->id, 'contract_version' => 1]
        );
        $this->toastKit->show($payload);
        return $this->id;
    }

    /** @internal */
    public function payload(): array
    {
        if (! isset($this->options['message'])) throw new InvalidArgumentException('A toast message is required before show().');
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

    private function defaults(): array
    {
        return [
            'title' => null,
            'variant' => 'neutral',
            'position' => 'bottom',
            'duration' => 3000,
            'persistent' => false,
            'animation' => 'spring',
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
}
