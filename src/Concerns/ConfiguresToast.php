<?php

namespace Victorycodedev\ToastKit\Concerns;

use BackedEnum;
use Victorycodedev\ToastKit\Enums\ToastAnimation;
use Victorycodedev\ToastKit\Enums\ToastDirection;
use Victorycodedev\ToastKit\Enums\ToastPosition;
use Victorycodedev\ToastKit\Enums\ToastStrategy;
use Victorycodedev\ToastKit\Enums\ToastTextAlign;
use Victorycodedev\ToastKit\Enums\ToastTextSize;
use Victorycodedev\ToastKit\Enums\ToastTextWeight;
use Victorycodedev\ToastKit\Enums\ToastVariant;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

trait ConfiguresToast
{
    abstract protected function put(string $key, mixed $value): static;
    abstract protected function putStyle(string $key, mixed $value): static;
    abstract protected function putTypography(string $group, array $values): static;

    public function message(string $message): static
    {
        if (trim($message) === '') throw new InvalidToastConfigurationException('A toast message must not be empty.');
        return $this->put('message', $message);
    }

    public function title(?string $title): static
    {
        return $this->put('title', $title);
    }

    public function text(
        ?string $font = null,
        ToastTextSize|string|null $size = null,
        ToastTextWeight|string|null $weight = null,
        ToastTextAlign|string|null $align = null,
        ?bool $italic = null,
    ): static {
        return $this->typography('text', $font, $size, $weight, $align, $italic);
    }

    public function titleText(
        ?string $font = null,
        ToastTextSize|string|null $size = null,
        ToastTextWeight|string|null $weight = null,
        ToastTextAlign|string|null $align = null,
        ?bool $italic = null,
    ): static {
        return $this->typography('title_text', $font, $size, $weight, $align, $italic);
    }

    public function success(): static
    {
        return $this->variant(ToastVariant::Success);
    }
    public function error(): static
    {
        return $this->variant(ToastVariant::Error);
    }
    public function warning(): static
    {
        return $this->variant(ToastVariant::Warning);
    }
    public function info(): static
    {
        return $this->variant(ToastVariant::Info);
    }
    public function neutral(): static
    {
        return $this->variant(ToastVariant::Neutral);
    }

    public function variant(ToastVariant|string $variant): static
    {
        return $this->put('variant', $this->enumValue(ToastVariant::class, $variant, 'variant'));
    }

    public function icon(?string $name = null, ?string $ios = null, ?string $android = null): static
    {
        $icon = array_filter([
            'name' => $this->nullableNonEmpty($name, 'Icon name'),
            'ios' => $this->nullableNonEmpty($ios, 'iOS icon name'),
            'android' => $this->nullableNonEmpty($android, 'Android icon name'),
        ], static fn($value) => $value !== null);
        if ($icon === []) throw new InvalidToastConfigurationException('An icon name or platform override is required.');
        return $this->put('icon', $icon);
    }

    public function position(ToastPosition|string $position): static
    {
        return $this->put('position', $this->enumValue(ToastPosition::class, $position, 'position'));
    }

    public function native(bool $ios = true, bool $android = true): static
    {
        return $this->put('native', ['ios' => $ios, 'android' => $android]);
    }

    public function opacity(float $opacity = 0.8): static
    {
        return $this->put('opacity', max(0.0, min(1.0, $opacity)));
    }

    public function duration(int $milliseconds): static
    {
        if ($milliseconds <= 0) throw new InvalidToastConfigurationException('Toast duration must be greater than zero milliseconds.');
        $this->put('persistent', false);
        return $this->put('duration', $milliseconds);
    }

    public function persistent(bool $persistent = true): static
    {
        $this->put('persistent', $persistent);
        return $persistent ? $this->put('duration', null) : $this;
    }

    public function animation(ToastAnimation|string $animation): static
    {
        return $this->put('animation', $this->enumValue(ToastAnimation::class, $animation, 'animation'));
    }

    public function direction(ToastDirection|string $direction): static
    {
        return $this->put('direction', $this->enumValue(ToastDirection::class, $direction, 'direction'));
    }

    public function progress(int|float $progress): static
    {
        if (! is_finite((float) $progress)) throw new InvalidToastConfigurationException('Toast progress must be a finite number.');
        return $this->put('progress', max(0, min(100, $progress)));
    }

    public function loading(bool $loading = true): static
    {
        return $this->put('loading', $loading);
    }

    public function swipeToDismiss(bool $enabled = true): static
    {
        return $this->put('swipe_to_dismiss', $enabled);
    }
    public function dismissible(bool $enabled = true): static
    {
        return $this->put('dismissible', $enabled);
    }

    public function action(string $label, string $id): static
    {
        if (trim($label) === '' || trim($id) === '') throw new InvalidToastConfigurationException('Action label and ID must not be empty.');
        return $this->put('action', ['id' => $id, 'label' => $label]);
    }

    public function background(string $color): static
    {
        return $this->putStyle('background', $this->color($color));
    }
    public function foreground(string $color): static
    {
        return $this->putStyle('foreground', $this->color($color));
    }
    public function iconColor(string $color): static
    {
        return $this->putStyle('icon_color', $this->color($color));
    }
    public function actionColor(string $color): static
    {
        return $this->putStyle('action_color', $this->color($color));
    }

    public function cornerRadius(float $radius): static
    {
        if ($radius < 0) throw new InvalidToastConfigurationException('Corner radius must not be negative.');
        return $this->putStyle('corner_radius', $radius);
    }

    public function padding(float $padding): static
    {
        if ($padding < 0) throw new InvalidToastConfigurationException('Padding must not be negative.');
        return $this->putStyle('padding', $padding);
    }

    public function shadow(bool $enabled = true): static
    {
        return $this->putStyle('shadow', $enabled);
    }
    public function queue(): static
    {
        return $this->strategy(ToastStrategy::Queue);
    }
    public function stack(): static
    {
        return $this->strategy(ToastStrategy::Stack);
    }

    public function strategy(ToastStrategy|string $strategy): static
    {
        return $this->put('strategy', $this->enumValue(ToastStrategy::class, $strategy, 'strategy'));
    }

    public function maxVisible(int $count): static
    {
        if ($count < 1) throw new InvalidToastConfigurationException('Maximum visible toasts must be at least one.');
        return $this->put('max_visible', $count);
    }

    private function color(string $color): string
    {
        if (! preg_match('/^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/', $color)) {
            throw new InvalidToastConfigurationException('Colors must use #RGB, #RRGGBB, or #AARRGGBB hexadecimal format.');
        }
        return strtoupper($color);
    }

    private function enumValue(string $enum, BackedEnum|string $value, string $label): string
    {
        if ($value instanceof $enum) return $value->value;
        $case = $enum::tryFrom($value);
        if ($case === null) throw new InvalidToastConfigurationException("Invalid toast {$label}: {$value}.");
        return $case->value;
    }

    private function typography(
        string $group,
        ?string $font,
        ToastTextSize|string|null $size,
        ToastTextWeight|string|null $weight,
        ToastTextAlign|string|null $align,
        ?bool $italic,
    ): static {
        $values = [];

        if ($font !== null) $values['font'] = $this->nullableNonEmpty($font, 'Text font');
        if ($size !== null) $values['size'] = $this->enumValue(ToastTextSize::class, $size, 'text size');
        if ($weight !== null) $values['weight'] = $this->enumValue(ToastTextWeight::class, $weight, 'text weight');
        if ($align !== null) $values['align'] = $this->enumValue(ToastTextAlign::class, $align, 'text align');
        if ($italic !== null) $values['italic'] = $italic;

        if ($values === []) return $this;

        return $this->putTypography($group, $values);
    }

    private function nullableNonEmpty(?string $value, string $label): ?string
    {
        if ($value !== null && trim($value) === '') throw new InvalidToastConfigurationException("{$label} must not be empty.");
        return $value;
    }
}
