# Toastkit Plugin for NativePHP Mobile

Rich, customizable native toast notifications for NativePHP Mobile.

## Installation

```bash
composer require victorycodedev/toastkit
```

## Usage

```php
use Victorycodedev\Toastkit\Facades\Toastkit;

// Execute functionality
$result = Toastkit::execute(['option1' => 'value']);

// Get status
$status = Toastkit::getStatus();
```

## Listening for Events

```php
use Livewire\Attributes\On;

#[On('native:Victorycodedev\Toastkit\Events\ToastkitCompleted')]
public function handleToastkitCompleted($result, $id = null)
{
    // Handle the event
}
```

## License

MIT