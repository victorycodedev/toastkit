<?php

namespace Victorycodedev\Toastkit\Facades;

use Illuminate\Support\Facades\Facade;

/**
 * @method static mixed execute(array $options = [])
 * @method static object|null getStatus()
 *
 * @see \Victorycodedev\Toastkit\Toastkit
 */
class Toastkit extends Facade
{
    protected static function getFacadeAccessor(): string
    {
        return \Victorycodedev\Toastkit\Toastkit::class;
    }
}