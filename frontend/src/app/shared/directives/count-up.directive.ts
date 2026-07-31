import {
  Directive, ElementRef, Input, OnChanges, SimpleChanges, NgZone
} from '@angular/core';

@Directive({
  selector: '[appCountUp]',
  standalone: true
})
export class CountUpDirective implements OnChanges {
  @Input('appCountUp') target = 0;
  @Input() duration = 800;
  @Input() prefix = '';
  @Input() suffix = '';

  private frame: number | null = null;

  constructor(private el: ElementRef<HTMLElement>, private zone: NgZone) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['target']) {
      this.animate(0, Number(this.target) || 0);
    }
  }

  private animate(from: number, to: number) {
    if (this.frame !== null) cancelAnimationFrame(this.frame);

    const startTime = performance.now();
    const duration = this.duration;

    const step = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = Math.round(from + (to - from) * eased);

      this.el.nativeElement.textContent = this.prefix + current.toLocaleString('en-IN') + this.suffix;

      if (progress < 1) {
        this.frame = requestAnimationFrame(step);
      } else {
        // Pop animation
        this.el.nativeElement.classList.add('count-popped');
        setTimeout(() => this.el.nativeElement.classList.remove('count-popped'), 350);
      }
    };

    this.zone.runOutsideAngular(() => {
      this.frame = requestAnimationFrame(step);
    });
  }
}
