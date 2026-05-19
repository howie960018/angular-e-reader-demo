import {
  Component, ElementRef, ViewChild,
  HostListener, ChangeDetectionStrategy, ChangeDetectorRef, OnInit,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookService } from '../services/book.service';
import { AuthService } from '../services/auth.service';

const LOCKED_MARKER = '__LOCKED__';

@Component({
  selector: 'app-ebook',
  templateUrl: './ebook.component.html',
  styleUrls: ['./ebook.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbookComponent implements OnInit {
  @ViewChild('measureBox', { static: false }) measureBox!: ElementRef;

  bookId: string = '';
  bookTitle: string = '電子書閱讀器';

  fullText: string = '';
  paragraphs: string[] = [];
  pages: string[] = [];
  currentPage: number = 0;
  fontSize: number = 18;
  isDarkMode: boolean = false;
  isLoading: boolean = true;
  loadError: boolean = false;
  readingMode: 'single' | 'double' = 'double';
  jumpPage: number = 1;

  hasAccess: boolean = false;
  isPreview: boolean = false;
  previewLength: number = 1500;
  totalLength: number = 0;
  isLoggedIn: boolean = false;

  readonly MAX_HEIGHT: number = 580;
  readonly MAX_WIDTH: number = 420;

  constructor(
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    public router: Router,
    private bookService: BookService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isAuthenticated();
    this.route.params.subscribe(params => {
      this.bookId = params['id'];
      this.loadContent(this.bookId);
    });
  }

  loadContent(bookId: string): void {
    this.isLoading = true;
    this.loadError = false;
    this.cdr.markForCheck();

    this.bookService.getBookContent(bookId).subscribe({
      next: res => {
        this.bookTitle = res.bookTitle;
        this.hasAccess = res.hasAccess;
        this.previewLength = res.previewLength;
        this.totalLength = res.totalLength;
        this.isPreview = !res.hasAccess && res.totalLength > res.previewLength;

        this.fullText = res.content;
        this.paragraphs = this.fullText
          .split(/\n+/)
          .map(p => p.trim())
          .filter(p => p.length > 0);

        if (this.isPreview) {
          this.paragraphs.push(LOCKED_MARKER);
        }

        this.isLoading = false;
        setTimeout(() => { this.paginate(); }, 0);
      },
      error: () => {
        this.isLoading = false;
        this.loadError = true;
        this.cdr.markForCheck();
      }
    });
  }

  // ── 分頁 ──────────────────────────────────────
  get leftPage(): string {
    const p = this.pages[this.currentPage] ?? '';
    return p === LOCKED_MARKER ? '' : p;
  }

  get rightPage(): string {
    if (this.readingMode !== 'double') return '';
    const p = this.pages[this.currentPage + 1] ?? '';
    return p === LOCKED_MARKER ? '' : p;
  }

  get isLeftLocked(): boolean {
    return this.pages[this.currentPage] === LOCKED_MARKER;
  }

  get isRightLocked(): boolean {
    return this.readingMode === 'double' && this.pages[this.currentPage + 1] === LOCKED_MARKER;
  }

  get isAnyPageLocked(): boolean {
    return this.isLeftLocked || this.isRightLocked;
  }

  get displayPageNumber(): number {
    return this.readingMode === 'double'
      ? Math.floor(this.currentPage / 2) + 1
      : this.currentPage + 1;
  }

  get totalDisplayPages(): number {
    const visiblePages = this.isPreview ? this.pages.length - 1 : this.pages.length;
    return this.readingMode === 'double'
      ? Math.ceil(visiblePages / 2)
      : visiblePages;
  }

  get progressPercent(): number {
    if (this.pages.length <= 1) return 0;
    return (this.currentPage / (this.pages.length - 1)) * 100;
  }

  paginate(): void {
    if (!this.paragraphs || this.paragraphs.length === 0) return;

    this.pages = [];
    const el = this.measureBox.nativeElement;
    el.style.height = 'auto';
    el.style.width = `${this.MAX_WIDTH}px`;
    el.style.fontSize = `${this.fontSize}px`;
    el.innerText = '';

    let currentBlock = '';

    for (let i = 0; i < this.paragraphs.length; i++) {
      const pText = this.paragraphs[i];

      if (pText === LOCKED_MARKER) {
        if (currentBlock.trim()) this.pushPage(currentBlock);
        this.pages.push(LOCKED_MARKER);
        break;
      }

      const testBlock = currentBlock ? currentBlock + '\n\n' + pText : pText;
      el.innerText = testBlock;

      if (el.scrollHeight <= this.MAX_HEIGHT) {
        currentBlock = testBlock;
      } else {
        if (currentBlock.trim()) {
          this.pushPage(currentBlock);
          currentBlock = '';
          i--;
        } else {
          this.splitParagraph(pText, el, this.MAX_HEIGHT);
          currentBlock = '';
        }
      }
    }

    if (currentBlock.trim()) this.pushPage(currentBlock);

    this.fixBounds();
    this.cdr.markForCheck();
  }

  splitParagraph(pText: string, el: HTMLElement, maxHeight: number): void {
    const sentences = pText.split(/(?<=[。！？；\n])/g).filter(s => s.length > 0);
    let current = '';
    for (let i = 0; i < sentences.length; i++) {
      const test = current ? current + sentences[i] : sentences[i];
      el.innerText = test;
      if (el.scrollHeight <= maxHeight) {
        current = test;
      } else {
        if (current.trim()) { this.pushPage(current); current = ''; i--; }
        else { this.splitByChars(sentences[i], el, maxHeight); current = ''; }
      }
    }
    if (current.trim()) this.pushPage(current);
  }

  splitByChars(text: string, el: HTMLElement, maxHeight: number): void {
    let current = '';
    for (const char of Array.from(text)) {
      const test = current + char;
      el.innerText = test;
      if (el.scrollHeight <= maxHeight) { current = test; }
      else { if (current.trim()) this.pushPage(current); current = char; }
    }
    if (current.trim()) this.pushPage(current);
  }

  pushPage(text: string): void {
    if (text.trim()) this.pages.push(text);
  }

  fixBounds(): void {
    const max = this.pages.length > 0 ? this.pages.length - 1 : 0;
    if (this.currentPage > max) this.currentPage = max;
    if (this.currentPage < 0) this.currentPage = 0;
    if (this.readingMode === 'double' && this.currentPage % 2 !== 0) this.currentPage--;
  }

  // ── 導航 ──────────────────────────────────────
  next(): void {
    const step = this.readingMode === 'double' ? 2 : 1;
    if (this.currentPage + step < this.pages.length) {
      this.currentPage += step;
      this.cdr.markForCheck();
    }
  }

  prev(): void {
    const step = this.readingMode === 'double' ? 2 : 1;
    if (this.currentPage - step >= 0) {
      this.currentPage -= step;
      this.cdr.markForCheck();
    }
  }

  goToPage(): void {
    const idx = this.jumpPage - 1;
    if (idx >= 0 && idx < this.pages.length) {
      this.currentPage = idx;
      this.fixBounds();
      this.cdr.markForCheck();
    }
  }

  // ── 顯示設定 ───────────────────────────────────
  zoomIn(): void {
    if (this.fontSize < 40) this.repaginateKeepPosition(() => { this.fontSize += 2; });
  }

  zoomOut(): void {
    if (this.fontSize > 12) this.repaginateKeepPosition(() => { this.fontSize -= 2; });
  }

  repaginateKeepPosition(fn: () => void): void {
    const ratio = this.pages.length ? this.currentPage / this.pages.length : 0;
    fn();
    this.paginate();
    let newCurrent = Math.round(ratio * this.pages.length);
    if (this.readingMode === 'double' && newCurrent % 2 !== 0) newCurrent--;
    this.currentPage = newCurrent;
    this.fixBounds();
    this.cdr.markForCheck();
  }

  toggleMode(): void {
    this.readingMode = this.readingMode === 'double' ? 'single' : 'double';
    this.fixBounds();
    this.cdr.markForCheck();
  }

  toggleDark(): void {
    this.isDarkMode = !this.isDarkMode;
    this.cdr.markForCheck();
  }

  goToBuy(): void {
    this.router.navigate(['/book', this.bookId]);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  @HostListener('window:wheel', ['$event'])
  onScroll(event: WheelEvent): void {
    if (event.ctrlKey) {
      event.preventDefault();
      event.deltaY < 0 ? this.zoomIn() : this.zoomOut();
    }
  }

  @HostListener('contextmenu', ['$event'])
  disableRightClick(event: MouseEvent): void {
    event.preventDefault();
  }
}
