<?php

namespace Victorycodedev\ToastKit;

use InvalidArgumentException;
use Victorycodedev\ToastKit\Concerns\ConfiguresToast;

class PendingToastUpdate
{
    use ConfiguresToast;

    private array $changes = [];

    public function __construct(private readonly ToastKit $toastKit, private readonly string $id) {}

    public function show(): string
    {
        if ($this->changes === []) throw new InvalidArgumentException('At least one toast change is required before show().');
        $this->toastKit->applyUpdate($this->id, $this->changes);
        return $this->id;
    }

    /** @internal */
    public function payload(): array
    {
        return ['id' => $this->id, 'changes' => $this->changes];
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
