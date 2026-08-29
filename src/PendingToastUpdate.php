<?php

namespace Victorycodedev\ToastKit;

use Victorycodedev\ToastKit\Concerns\ConfiguresToast;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

class PendingToastUpdate
{
    use ConfiguresToast;

    private array $changes = [];

    public function __construct(
        private readonly ToastKit $toastKit,
        private readonly string $target,
        private readonly bool $usesUniqueKey = false,
    ) {}

    public function show(): string
    {
        if ($this->changes === []) throw new InvalidToastConfigurationException('At least one toast change is required before show().');
        if ($this->usesUniqueKey) {
            return $this->toastKit->applyUniqueUpdate($this->target, $this->changes) ?? $this->target;
        }
        $this->toastKit->applyUpdate($this->target, $this->changes);
        return $this->target;
    }

    /** @internal */
    public function payload(): array
    {
        return [
            $this->usesUniqueKey ? 'unique_key' : 'id' => $this->target,
            'changes' => $this->changes,
        ];
    }

    protected function put(string $key, mixed $value): static
    {
        $this->changes[$key] = $value;
        return $this;
    }

    protected function putStyle(string $key, mixed $value): static
    {
        $this->changes['style'][$key] = $value;
        return $this;
    }

    protected function putTypography(string $group, array $values): static
    {
        $this->changes[$group] = array_merge($this->changes[$group] ?? [], $values);
        return $this;
    }
}
