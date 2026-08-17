## victorycodedev/toastkit

Rich, customizable native toast notifications for NativePHP Mobile.

### Installation

```bash
composer require victorycodedev/toastkit
```

### PHP Usage (Livewire/Blade)

Use the `Toastkit` facade:

@verbatim
<code-snippet name="Using Toastkit Facade" lang="php">
use Victorycodedev\Toastkit\Facades\Toastkit;

// Execute the plugin functionality
$result = Toastkit::execute(['option1' => 'value']);

// Get the current status
$status = Toastkit::getStatus();
</code-snippet>
@endverbatim

### Available Methods

- `Toastkit::execute()`: Execute the plugin functionality
- `Toastkit::getStatus()`: Get the current status

### Events

- `ToastkitCompleted`: Listen with `#[OnNative(ToastkitCompleted::class)]`

@verbatim
<code-snippet name="Listening for Toastkit Events" lang="php">
use Native\Mobile\Attributes\OnNative;
use Victorycodedev\Toastkit\Events\ToastkitCompleted;

#[OnNative(ToastkitCompleted::class)]
public function handleToastkitCompleted($result, $id = null)
{
    // Handle the event
}
</code-snippet>
@endverbatim

### JavaScript Usage (Vue/React/Inertia)

@verbatim
<code-snippet name="Using Toastkit in JavaScript" lang="javascript">
import { toastkit } from '@victorycodedev/toastkit';

// Execute the plugin functionality
const result = await toastkit.execute({ option1: 'value' });

// Get the current status
const status = await toastkit.getStatus();
</code-snippet>
@endverbatim