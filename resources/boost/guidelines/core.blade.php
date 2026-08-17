## ToastKit

Use the `Toast` facade to create native toast contracts:

<code-snippet name="Showing a ToastKit toast" lang="php">
    use Victorycodedev\ToastKit\Facades\Toast;

    $id = Toast::success('Changes saved')->show();
    Toast::update($id)->message('Updated')->show();
    Toast::dismiss($id);
</code-snippet>

ToastKit currently defines the PHP and native bridge contract. Native rendering is not implemented yet.

Listen for `ToastShown`, `ToastDismissed`, and `ToastActionPressed` with NativePHP's `#[On]` attribute.
